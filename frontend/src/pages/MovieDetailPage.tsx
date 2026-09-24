import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { discoveryApi, getApiError, movieApi, userDataApi } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { Button, QueryError, Spinner } from '../components/ui'
import { PosterArtwork } from '../components/MovieCard'
import { VideoPlayer, type PlaybackMode } from '../components/VideoPlayer'
import { useToast } from '../components/ToastProvider'
import { formatNumber } from '../lib/format'
import { Seo } from '../components/Seo'
import { useI18n } from '../lib/i18n'

export function MovieDetailPage() {
  const { t, locale, categoryName } = useI18n()
  const { id } = useParams()
  const movieId = Number(id)
  const client = useQueryClient()
  const { push } = useToast()
  const user = useAuthStore((state) => state.user)
  const activeProfileId = useAuthStore((state) => state.activeProfileId)
  const [playbackMode, setPlaybackMode] = useState<PlaybackMode | null>(null)

  const movie = useQuery({ queryKey: ['movie', movieId], queryFn: () => movieApi.get(movieId), enabled: Number.isFinite(movieId) })
  const watchlist = useQuery({ queryKey: ['watchlist', activeProfileId], queryFn: userDataApi.watchlist, enabled: Boolean(user && activeProfileId) })
  const favourites = useQuery({ queryKey: ['favourites', activeProfileId], queryFn: userDataApi.favourites, enabled: Boolean(user && activeProfileId) })
  const continueQuery = useQuery({ queryKey: ['continue-watching', activeProfileId], queryFn: userDataApi.continueWatching, enabled: Boolean(user && activeProfileId) })
  const preferences = useQuery({ queryKey: ['preferences', activeProfileId], queryFn: discoveryApi.preferences, enabled: Boolean(user && activeProfileId) })
  const action = useMutation({
    mutationFn: async (type: 'watchlist' | 'favourites' | 'history') => {
      if (type === 'watchlist') return watchlist.data?.some((item) => item.movieId === movieId) ? userDataApi.removeWatchlist(movieId) : userDataApi.addWatchlist(movieId)
      if (type === 'favourites') return favourites.data?.some((item) => item.movieId === movieId) ? userDataApi.removeFavourite(movieId) : userDataApi.addFavourite(movieId)
      return userDataApi.addHistory(movieId)
    },
    onSuccess: (_, type) => { client.invalidateQueries({ queryKey: [type === 'watchlist' ? 'watchlist' : type === 'favourites' ? 'favourites' : 'history'] }); push(type === 'history' ? t('movie.addedHistory') : type === 'watchlist' ? t('movie.watchlistUpdated') : t('movie.favouritesUpdated'), 'success') },
    onError: (error) => push(getApiError(error), 'error'),
  })
  const preferenceAction = useMutation({
    mutationFn: async (signal: 'LIKE' | 'DISLIKE') => { if (preferences.data?.find((entry) => entry.movieId === movieId)?.signal === signal) await discoveryApi.removePreference(movieId); else await discoveryApi.setPreference(movieId, signal) },
    onSuccess: () => { client.invalidateQueries({ queryKey: ['preferences'] }); push(t('movie.preferenceSaved'), 'success') },
    onError: (error) => push(getApiError(error), 'error'),
  })
  if (movie.isLoading) return <Spinner label={t('movie.loading')} />
  if (movie.isError || !movie.data) return <QueryError message={getApiError(movie.error)} />

  const item = movie.data
  const inWatchlist = watchlist.data?.some((entry) => entry.movieId === movieId)
  const inFavourites = favourites.data?.some((entry) => entry.movieId === movieId)
  const preference = preferences.data?.find((entry) => entry.movieId === movieId)?.signal
  const saved = continueQuery.data?.find((entry) => entry.movieId === movieId)
  const activeSource = playbackMode === 'WATCH' ? item.videoUrl : item.trailerUrl
  const synopsis = item.description?.trim()
  const cast = item.actors?.split(',').map((name) => name.trim()).filter(Boolean) || []
  const openPlayer = (mode: PlaybackMode) => {
    const source = mode === 'WATCH' ? item.videoUrl : item.trailerUrl
    if (!source) { push(mode === 'WATCH' ? t('movie.noWatchSource') : t('movie.noTrailer'), 'info'); return }
    setPlaybackMode(mode)
  }

  return <div className="space-y-8">
    <Seo title={`${item.title} · Cinevora`} description={synopsis || t('movie.noSynopsis')} image={item.thumbnailUrl} jsonLd={{ '@context': 'https://schema.org', '@type': 'Movie', name: item.title, description: synopsis || undefined, image: item.thumbnailUrl || undefined, dateCreated: String(item.releaseYear), aggregateRating: { '@type': 'AggregateRating', ratingValue: item.rating, bestRating: 10, ratingCount: Math.max(item.views, 1) } }} />
    <Link to="/browse" className="back-link">{t('movie.back')}</Link>
    <section className="detail-hero">
      <div className="detail-poster"><PosterArtwork src={item.thumbnailUrl} alt={t('common.poster', { title: item.title })} title={item.title} /></div>
      <div className="detail-copy">
        <div className="detail-meta"><span>{categoryName(item.categoryName)}</span><span>{item.releaseYear}</span><span>{item.durationMinutes ? t('common.minutes', { count: item.durationMinutes }) : t('movie.feature')}</span></div>
        <h2 className="mt-4 text-4xl font-semibold tracking-tight text-white md:text-6xl">{item.title}</h2>
        <div className="detail-engagement"><span><b>★ {item.rating.toFixed(1)}</b> {t('movie.rating')}</span><span>{t('common.views', { count: formatNumber(item.views, locale) })}</span><span>{t('common.favourites', { count: formatNumber(item.favouritesCount, locale) })}</span></div>
        <div className="detail-actions">
          <div><p className="detail-action-label">{t('movie.playback')}</p><div className="flex flex-wrap gap-3"><Button onClick={() => openPlayer('WATCH')} disabled={!item.videoUrl}>{saved ? t('movie.resume') : t('movie.watchNow')}</Button><Button variant="secondary" onClick={() => openPlayer('TRAILER')} disabled={!item.trailerUrl}>{t('movie.trailer')}</Button></div></div>
          <div><p className="detail-action-label">{t('movie.saveAndRate')}</p><div className="flex flex-wrap gap-2"><Button variant="secondary" onClick={() => user && action.mutate('watchlist')} aria-pressed={inWatchlist}>{inWatchlist ? t('movie.inWatchlist') : t('movie.addWatchlist')}</Button><Button variant="ghost" onClick={() => user && action.mutate('favourites')} aria-pressed={inFavourites}>{t('movie.favourite')}{inFavourites ? ' ✓' : ''}</Button><Button variant={preference === 'LIKE' ? 'primary' : 'ghost'} onClick={() => user && preferenceAction.mutate('LIKE')} aria-pressed={preference === 'LIKE'}>{t('movie.like')}</Button><Button variant={preference === 'DISLIKE' ? 'danger' : 'ghost'} onClick={() => user && preferenceAction.mutate('DISLIKE')} aria-pressed={preference === 'DISLIKE'}>{t('movie.dislike')}</Button><Button variant="ghost" onClick={() => user ? action.mutate('history') : push(t('movie.signInTrack'), 'info')}>{t('movie.markWatched')}</Button></div></div>
        </div>
      </div>
    </section>
    {playbackMode && activeSource && <section className="space-y-3"><div className="flex items-center justify-between"><div><p className="eyebrow">{playbackMode === 'WATCH' ? t('movie.watchNow') : t('movie.trailer')}</p><h3 className="text-xl font-semibold text-white">{item.title}</h3></div><Button variant="ghost" onClick={() => { const wasWatching = playbackMode === 'WATCH'; setPlaybackMode(null); if (wasWatching) client.invalidateQueries({ queryKey: ['continue-watching'] }) }}>{t('movie.closePlayer')}</Button></div><VideoPlayer movieId={movieId} mode={playbackMode} src={activeSource} initialPositionSeconds={playbackMode === 'WATCH' ? (saved?.positionSeconds || 0) : 0} /></section>}
    <section className="detail-layout">
      <div className="space-y-5">
        <section className="surface detail-section"><p className="eyebrow">{t('movie.synopsis')}</p><p className={`mt-4 max-w-3xl text-base leading-7 ${synopsis ? 'text-slate-300' : 'text-slate-500'}`}>{synopsis || t('movie.noSynopsis')}</p></section>
        <section className="surface detail-section"><p className="eyebrow">{t('movie.people')}</p><div className="mt-5 grid gap-6 sm:grid-cols-[minmax(150px,.6fr)_1.4fr]"><div><p className="text-xs uppercase tracking-widest text-slate-500">{t('movie.director')}</p><p className={`mt-2 font-medium ${item.director?.trim() ? 'text-white' : 'text-slate-500'}`}>{item.director?.trim() || t('movie.notListed')}</p></div><div><p className="text-xs uppercase tracking-widest text-slate-500">{t('movie.cast')}</p>{cast.length ? <ul className="cast-list">{cast.map((name, index) => <li key={`${name}-${index}`}>{name}</li>)}</ul> : <p className="mt-2 text-sm text-slate-500">{t('movie.noCast')}</p>}</div></div></section>
      </div>
      <aside className="surface detail-section"><p className="eyebrow">{t('movie.titleDetails')}</p><dl className="detail-facts"><div><dt>{t('movie.genre')}</dt><dd>{categoryName(item.categoryName)}</dd></div><div><dt>{t('movie.year')}</dt><dd>{item.releaseYear}</dd></div><div><dt>{t('movie.duration')}</dt><dd>{item.durationMinutes ? t('common.minutes', { count: item.durationMinutes }) : t('movie.notListed')}</dd></div><div><dt>{t('movie.rating')}</dt><dd>★ {item.rating.toFixed(1)} / 10</dd></div></dl><div className="mt-7 border-t border-slate-800 pt-6"><p className="eyebrow">{t('movie.progress')}</p><p className="mt-2 text-sm leading-6 text-slate-400">{t('movie.progressCopy')}</p>{saved && <p className="mt-5 text-sm text-pink-200">{t('movie.resumeProgress', { minutes: Math.floor(saved.positionSeconds / 60), percent: saved.percent })}</p>}<Button className="mt-4 w-full" variant="secondary" onClick={() => openPlayer('WATCH')} disabled={!item.videoUrl}>{item.videoUrl ? (saved ? t('movie.resume') : t('movie.openPlayer')) : t('movie.awaitingSource')}</Button></div></aside>
    </section>
  </div>
}
