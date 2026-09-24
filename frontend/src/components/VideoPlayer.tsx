import { useCallback, useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { trackApi, userDataApi } from "../lib/api";
import { Button } from "./ui";
import { useI18n } from "../lib/i18n";

type SubtitleTrack = {
  src: string;
  srclang: string;
  label: string;
  default?: boolean;
};

export type PlaybackMode = "WATCH" | "TRAILER";

function getYouTubeVideoId(source: string) {
  try {
    const url = new URL(source);
    const hostname = url.hostname.toLowerCase().replace(/^www\./, "");
    const id =
      hostname === "youtu.be"
        ? url.pathname.split("/").filter(Boolean)[0]
        : hostname === "youtube.com" || hostname === "youtube-nocookie.com"
          ? url.searchParams.get("v") ||
            url.pathname.match(/^\/(?:embed|shorts)\/([^/?]+)/)?.[1]
          : null;
    return id && /^[A-Za-z0-9_-]{11}$/.test(id) ? id : null;
  } catch {
    return null;
  }
}

export function VideoPlayer({
  movieId,
  src,
  mode,
  initialPositionSeconds = 0,
  onSaved,
  subtitles = [],
}: {
  movieId: number;
  src: string;
  mode: PlaybackMode;
  initialPositionSeconds?: number;
  onSaved?: () => void;
  subtitles?: SubtitleTrack[];
}) {
  const { t } = useI18n();
  const videoRef = useRef<HTMLVideoElement>(null);
  const lastSavedAt = useRef(0);
  const lastSavedPosition = useRef(-1);
  const saveInFlight = useRef(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [rate, setRate] = useState(1);
  const [muted, setMuted] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [error, setError] = useState("");
  const [audioTrackId, setAudioTrackId] = useState<number | null>(null);
  const tracks = useQuery({
    queryKey: ["movie-tracks", movieId],
    queryFn: () => trackApi.list(movieId),
    staleTime: 300_000,
    enabled: mode === "WATCH",
  });
  const audioTracks =
    mode === "WATCH"
      ? tracks.data?.filter((track) => track.kind === "AUDIO") || []
      : [];
  const selectedAudio =
    audioTracks.find((track) => track.id === audioTrackId) ||
    audioTracks.find((track) => track.defaultTrack);
  const playbackSrc = selectedAudio?.sourceUrl || src;
  const youtubeVideoId = getYouTubeVideoId(playbackSrc);

  useEffect(() => {
    if (audioTrackId === null && audioTracks.length > 0)
      setAudioTrackId(
        audioTracks.find((track) => track.defaultTrack)?.id ||
          audioTracks[0].id,
      );
  }, [audioTrackId, audioTracks]);

  const saveProgress = useCallback(
    async (force = false) => {
      const video = videoRef.current;
      if (
        mode !== "WATCH" ||
        !video ||
        !Number.isFinite(video.duration) ||
        video.duration <= 0 ||
        saveInFlight.current
      )
        return;
      const position = Math.max(0, Math.floor(video.currentTime));
      if (!force && Math.abs(position - lastSavedPosition.current) < 1) return;
      saveInFlight.current = true;
      try {
        await userDataApi.updateProgress(
          movieId,
          position,
          Math.floor(video.duration),
        );
        lastSavedAt.current = Date.now();
        lastSavedPosition.current = position;
        onSaved?.();
      } catch {
        setError(
          t("player.progressError"),
        );
      } finally {
        saveInFlight.current = false;
      }
    },
    [mode, movieId, onSaved, t],
  );

  useEffect(() => {
    if (mode !== "WATCH") return;
    const handleVisibility = () => {
      if (document.visibilityState === "hidden") void saveProgress(true);
    };
    const handlePageHide = () => {
      void saveProgress(true);
    };
    document.addEventListener("visibilitychange", handleVisibility);
    window.addEventListener("pagehide", handlePageHide);
    return () => {
      document.removeEventListener("visibilitychange", handleVisibility);
      window.removeEventListener("pagehide", handlePageHide);
      void saveProgress(true);
    };
  }, [mode, saveProgress]);

  if (youtubeVideoId && mode === "TRAILER") {
    return (
      <div className="video-player surface">
        <div className="relative aspect-video overflow-hidden rounded-2xl bg-black">
          <iframe
            className="h-full w-full"
            src={`https://www.youtube-nocookie.com/embed/${youtubeVideoId}?rel=0&modestbranding=1&playsinline=1`}
            title="Official movie trailer"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            referrerPolicy="strict-origin-when-cross-origin"
            allowFullScreen
          />
        </div>
        <p className="video-caption-note mt-3">
          {t("player.trailerNotice")}
        </p>
      </div>
    );
  }

  if (youtubeVideoId && mode === "WATCH") {
    return (
      <div className="video-player surface">
        <p className="video-error" role="status">
          {t("player.watchNeedsDirect")}
        </p>
      </div>
    );
  }

  const onLoadedMetadata = () => {
    const video = videoRef.current;
    if (!video) return;
    setDuration(video.duration);
    if (
      mode === "WATCH" &&
      initialPositionSeconds > 0 &&
      initialPositionSeconds < video.duration * 0.9
    )
      video.currentTime = initialPositionSeconds;
  };
  const onTimeUpdate = () => {
    const video = videoRef.current;
    if (!video) return;
    setCurrentTime(video.currentTime);
    if (Date.now() - lastSavedAt.current >= 7000) void saveProgress();
  };
  const seek = (value: number) => {
    if (videoRef.current) videoRef.current.currentTime = value;
    void saveProgress(true);
  };
  const togglePlay = () => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) void video.play();
    else {
      video.pause();
      void saveProgress(true);
    }
  };
  const formatTime = (seconds: number) => {
    if (!Number.isFinite(seconds)) return "00:00";
    const total = Math.max(0, Math.floor(seconds));
    return `${String(Math.floor(total / 3600)).padStart(2, "0")}:${String(Math.floor((total % 3600) / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
  };

  return (
    <div className="video-player surface">
      <video
        ref={videoRef}
        className="video-element"
        src={playbackSrc}
        controls
        playsInline
        preload="metadata"
        onLoadedMetadata={onLoadedMetadata}
        onPlay={() => setIsPlaying(true)}
        onTimeUpdate={onTimeUpdate}
        onPause={() => {
          setIsPlaying(false);
          void saveProgress(true);
        }}
        onSeeked={() => void saveProgress(true)}
        onEnded={() => {
          setIsPlaying(false);
          void saveProgress(true);
        }}
        onError={() => setError(t("player.videoError"))}
      >
        {subtitles.map((track) => (
          <track
            key={`${track.srclang}-${track.src}`}
            kind="subtitles"
            src={track.src}
            srcLang={track.srclang}
            label={track.label}
            default={track.default}
          />
        ))}
      </video>
      <div className="video-toolbar">
        <Button type="button" variant="secondary" onClick={togglePlay}>
          {isPlaying ? t("player.pause") : t("player.play")}
        </Button>
        <label className="video-time">
          {formatTime(currentTime)} / {formatTime(duration)}
          <input
            aria-label={t("player.seek")}
            type="range"
            min="0"
            max={duration || 0}
            step="1"
            value={Math.min(currentTime, duration || 0)}
            onChange={(event) => seek(Number(event.target.value))}
          />
        </label>
        <Button
          type="button"
          variant="ghost"
          onClick={() => {
            if (videoRef.current)
              videoRef.current.muted = !videoRef.current.muted;
            setMuted(!muted);
          }}
        >
          {muted ? t("player.unmute") : t("player.mute")}
        </Button>
        {mode === "WATCH" && audioTracks.length > 0 && (
          <label className="video-rate">
            {t("player.audio")}
            <select
              aria-label={t("player.audioTrack")}
              value={selectedAudio?.id || ""}
              onChange={(event) => setAudioTrackId(Number(event.target.value))}
            >
              <option value="">{t("player.original")}</option>
              {audioTracks.map((track) => (
                <option key={track.id} value={track.id}>
                  {track.label}
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="video-rate">
          {t("player.speed")}
          <select
            aria-label={t("player.speedLabel")}
            value={rate}
            onChange={(event) => {
              const next = Number(event.target.value);
              setRate(next);
              if (videoRef.current) videoRef.current.playbackRate = next;
            }}
          >
            <option value="0.75">0.75×</option>
            <option value="1">1×</option>
            <option value="1.25">1.25×</option>
            <option value="1.5">1.5×</option>
            <option value="2">2×</option>
          </select>
        </label>
        {subtitles.length > 0 && (
          <span className="video-caption-note">
            {t("player.subtitles")}
          </span>
        )}
      </div>
      {mode === "TRAILER" && (
        <p className="video-caption-note mt-3">
          {t("player.trailerNotice")}
        </p>
      )}
      {error && (
        <p className="video-error" role="status">
          {error}
        </p>
      )}
    </div>
  );
}
