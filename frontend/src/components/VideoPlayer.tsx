import { useCallback, useEffect, useRef, useState } from 'react'
import { userDataApi } from '../lib/api'
import { Button } from './ui'

type SubtitleTrack = { src: string; srclang: string; label: string; default?: boolean }

export function VideoPlayer({
  movieId,
  src,
  initialPositionSeconds = 0,
  onSaved,
  subtitles = [],
}: {
  movieId: number
  src: string
  initialPositionSeconds?: number
  onSaved?: () => void
  subtitles?: SubtitleTrack[]
}) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const lastSavedAt = useRef(0)
  const lastSavedPosition = useRef(-1)
  const saveInFlight = useRef(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [rate, setRate] = useState(1)
  const [muted, setMuted] = useState(false)
  const [isPlaying, setIsPlaying] = useState(false)
  const [error, setError] = useState('')

  const saveProgress = useCallback(async (force = false) => {
    const video = videoRef.current
    if (!video || !Number.isFinite(video.duration) || video.duration <= 0 || saveInFlight.current) return
    const position = Math.max(0, Math.floor(video.currentTime))
    if (!force && Math.abs(position - lastSavedPosition.current) < 1) return
    saveInFlight.current = true
    try {
      await userDataApi.updateProgress(movieId, position, Math.floor(video.duration))
      lastSavedAt.current = Date.now()
      lastSavedPosition.current = position
      onSaved?.()
    } catch {
      setError('Progress could not be saved. We will retry on your next playback event.')
    } finally {
      saveInFlight.current = false
    }
  }, [movieId, onSaved])

  useEffect(() => {
    const handleVisibility = () => { if (document.visibilityState === 'hidden') void saveProgress(true) }
    const handlePageHide = () => { void saveProgress(true) }
    document.addEventListener('visibilitychange', handleVisibility)
    window.addEventListener('pagehide', handlePageHide)
    return () => {
      document.removeEventListener('visibilitychange', handleVisibility)
      window.removeEventListener('pagehide', handlePageHide)
      void saveProgress(true)
    }
  }, [saveProgress])

  const onLoadedMetadata = () => {
    const video = videoRef.current
    if (!video) return
    setDuration(video.duration)
    if (initialPositionSeconds > 0 && initialPositionSeconds < video.duration * 0.9) video.currentTime = initialPositionSeconds
  }
  const onTimeUpdate = () => {
    const video = videoRef.current
    if (!video) return
    setCurrentTime(video.currentTime)
    if (Date.now() - lastSavedAt.current >= 7000) void saveProgress()
  }
  const seek = (value: number) => {
    if (videoRef.current) videoRef.current.currentTime = value
    void saveProgress(true)
  }
  const togglePlay = () => {
    const video = videoRef.current
    if (!video) return
    if (video.paused) void video.play()
    else { video.pause(); void saveProgress(true) }
  }
  const formatTime = (seconds: number) => {
    if (!Number.isFinite(seconds)) return '00:00'
    const total = Math.max(0, Math.floor(seconds))
    return `${String(Math.floor(total / 3600)).padStart(2, '0')}:${String(Math.floor((total % 3600) / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
  }

  return <div className="video-player surface">
    <video ref={videoRef} className="video-element" src={src} controls playsInline preload="metadata" onLoadedMetadata={onLoadedMetadata} onPlay={() => setIsPlaying(true)} onTimeUpdate={onTimeUpdate} onPause={() => { setIsPlaying(false); void saveProgress(true) }} onSeeked={() => void saveProgress(true)} onEnded={() => { setIsPlaying(false); void saveProgress(true) }} onError={() => setError('This video source could not be loaded.')}>
      {subtitles.map((track) => <track key={`${track.srclang}-${track.src}`} kind="subtitles" src={track.src} srcLang={track.srclang} label={track.label} default={track.default} />)}
    </video>
    <div className="video-toolbar">
      <Button type="button" variant="secondary" onClick={togglePlay}>{isPlaying ? 'Pause' : 'Play'}</Button>
      <label className="video-time">{formatTime(currentTime)} / {formatTime(duration)}<input aria-label="Seek video" type="range" min="0" max={duration || 0} step="1" value={Math.min(currentTime, duration || 0)} onChange={(event) => seek(Number(event.target.value))} /></label>
      <Button type="button" variant="ghost" onClick={() => { if (videoRef.current) videoRef.current.muted = !videoRef.current.muted; setMuted(!muted) }}>{muted ? 'Unmute' : 'Mute'}</Button>
      <label className="video-rate">Speed<select aria-label="Playback speed" value={rate} onChange={(event) => { const next = Number(event.target.value); setRate(next); if (videoRef.current) videoRef.current.playbackRate = next }}><option value="0.75">0.75×</option><option value="1">1×</option><option value="1.25">1.25×</option><option value="1.5">1.5×</option><option value="2">2×</option></select></label>
      {subtitles.length > 0 && <span className="video-caption-note">Subtitles available in player controls</span>}
    </div>
    {error && <p className="video-error" role="status">{error}</p>}
  </div>
}
