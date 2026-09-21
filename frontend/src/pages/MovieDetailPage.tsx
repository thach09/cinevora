import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { discoveryApi, getApiError, movieApi, userDataApi } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { Button, QueryError, Spinner } from '../components/ui'
import { PosterArtwork } from '../components/MovieCard'
import { VideoPlayer } from '../components/VideoPlayer'
import { useToast } from '../components/ToastProvider'
import { formatNumber } from '../lib/format'
import { Seo } from '../components/Seo'

export function MovieDetailPage() {
  const { id } = useParams()
  const movieId = Number(id)
  const client = useQueryClient()
  const { push } = useToast()
  const user = useAuthStore((state) => state.user)
  const activeProfileId = useAuthStore((state) => state.activeProfileId)
  const [showPlayer, setShowPlayer] = useState(false)
  const movie = useQuery({ queryKey: ['movie', movieId], queryFn: () => movieApi.get(movieId), enabled: Number.isFinite(movieId) })
  const watchlist = useQuery({ queryKey: ['watchlist', activeProfileId], queryFn: userDataApi.watchlist, enabled: Boolean(user && activeProfileId) })
  const favourites = useQuery({ queryKey: ['favourites', activeProfileId], queryFn: userDataApi.favourites, enabled: Boolean(user && activeProfileId) })
  const continueQuery = useQuery({ queryKey: ['continue-watching', activeProfileId], queryFn: userDataApi.continueWatching, enabled: Boolean(user && activeProfileId) })
  const preferences = useQuery({ queryKey: ['preferences', activeProfileId], queryFn: discoveryApi.preferences, enabled: Boolean(user && activeProfileId) })
  const action = useMutation({
    mutationFn: async (type: 'watchlist' | 'favourites' | 'history') => {
      if (type === 'watchlist') {
        const exists = watchlist.data?.some((item) => item.movieId === movieId)
        return exists ? userDataApi.removeWatchlist(movieId) : userDataApi.addWatchlist(movieId)
      }
      if (type === 'favourites') {
        const exists = favourites.data?.some((item) => item.movieId === movieId)
        return exists ? userDataApi.removeFavourite(movieId) : userDataApi.addFavourite(movieId)
      }
      return userDataApi.addHistory(movieId)
    },
    onSuccess: (_, type) => { client.invalidateQueries({ queryKey: [type === 'watchlist' ? 'watchlist' : type === 'favourites' ? 'favourites' : 'history'] }); push(type === 'history' ? 'Added to your watch history.' : `Updated your ${type}.`, 'success') },
    onError: (error) => push(getApiError(error), 'error'),
  })
  const preferenceAction = useMutation({
    mutationFn: async (signal: 'LIKE' | 'DISLIKE') => { if (preferences.data?.find((entry) => entry.movieId === movieId)?.signal === signal) await discoveryApi.removePreference(movieId); else await discoveryApi.setPreference(movieId, signal) },
    onSuccess: () => { client.invalidateQueries({ queryKey: ['preferences'] }); push('Your preference has been saved.', 'success') },
    onError: (error) => push(getApiError(error), 'error'),
  })

  if (movie.isLoading) return <Spinner label="Loading movie details" />
  if (movie.isError || !movie.data) return <QueryError message={getApiError(movie.error)} />

  const item = movie.data
  const inWatchlist = watchlist.data?.some((entry) => entry.movieId === movieId)
  const inFavourites = favourites.data?.some((entry) => entry.movieId === movieId)
  const preference = preferences.data?.find((entry) => entry.movieId === movieId)?.signal
  const saved = continueQuery.data?.find((entry) => entry.movieId === movieId)
  const openPlayer = () => {
    if (!item.videoUrl) { push('This title does not have a video source yet.', 'info'); return }
    setShowPlayer(true)
  }

  return <div className="space-y-8">
    <Seo title={`${item.title} · Cinevora`} description={item.description || `Watch ${item.title} on Cinevora.`} image={item.thumbnailUrl} jsonLd={{ '@context': 'https://schema.org', '@type': 'Movie', name: item.title, description: item.description || undefined, image: item.thumbnailUrl || undefined, dateCreated: String(item.releaseYear), aggregateRating: { '@type': 'AggregateRating', ratingValue: item.rating, bestRating: 10, ratingCount: Math.max(item.views, 1) } }} />
    <Link to="/browse" className="back-link">Back to browse</Link>
    <section className="detail-hero">
      <div className="detail-poster"><PosterArtwork src={item.thumbnailUrl} alt={`${item.title} poster`} title={item.title} /></div>
      <div className="detail-copy">
        <div className="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-[0.18em] text-pink-300"><span>{item.categoryName}</span><span className="text-slate-600">/</span><span>{item.releaseYear}</span><span className="text-slate-600">/</span><span>{item.durationMinutes ? `${item.durationMinutes} min` : 'Feature'}</span></div>
        <h2 className="mt-4 text-4xl font-semibold tracking-tight text-white md:text-6xl">{item.title}</h2>
        <p className="mt-5 max-w-2xl text-base leading-7 text-slate-300">{item.description || 'A story waiting to be discovered.'}</p>
        <div className="mt-6 flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-400"><span>★ <b className="text-white">{item.rating.toFixed(1)}</b> rating</span><span>{formatNumber(item.views)} views</span><span>{formatNumber(item.favouritesCount)} favourites</span></div>
        <div className="mt-8 flex flex-wrap gap-3"><Button onClick={openPlayer}>{item.videoUrl ? 'Play now' : 'No video source'}</Button><Button variant="secondary" onClick={() => user && action.mutate('watchlist')}>{inWatchlist ? 'In watchlist' : '+ Add to watchlist'}</Button><Button variant="ghost" onClick={() => user && action.mutate('favourites')}>{inFavourites ? 'Favourite' : 'Favourite'}</Button><Button variant={preference === 'LIKE' ? 'primary' : 'ghost'} onClick={() => user && preferenceAction.mutate('LIKE')} aria-pressed={preference === 'LIKE'}>Like</Button><Button variant={preference === 'DISLIKE' ? 'danger' : 'ghost'} onClick={() => user && preferenceAction.mutate('DISLIKE')} aria-pressed={preference === 'DISLIKE'}>Dislike</Button><Button variant="ghost" onClick={() => user ? action.mutate('history') : push('Sign in to track your viewing.', 'info')}>Mark as watched</Button></div>
      </div>
    </section>
    {showPlayer && item.videoUrl && <section className="space-y-3"><div className="flex items-center justify-between"><div><p className="eyebrow">Now playing</p><h3 className="text-xl font-semibold text-white">{item.title}</h3></div><Button variant="ghost" onClick={() => { setShowPlayer(false); client.invalidateQueries({ queryKey: ['continue-watching'] }) }}>Close player</Button></div><VideoPlayer movieId={movieId} src={item.videoUrl} initialPositionSeconds={saved?.positionSeconds || 0} /></section>}
    <section className="grid gap-5 md:grid-cols-3">
      <div className="surface p-6 md:col-span-2"><p className="eyebrow">The people behind it</p><div className="mt-4 grid gap-5 sm:grid-cols-2"><div><p className="text-xs uppercase tracking-widest text-slate-500">Director</p><p className="mt-1 font-medium text-white">{item.director}</p></div><div><p className="text-xs uppercase tracking-widest text-slate-500">Cast</p><p className="mt-1 leading-6 text-slate-300">{item.actors}</p></div></div></div>
      <div className="surface p-6"><p className="eyebrow">Continue watching</p><p className="mt-2 text-sm text-slate-400">Playback saves every few seconds and when you pause, seek, switch tab, or leave.</p>{saved && <p className="mt-5 text-sm text-pink-200">Resume at {Math.floor(saved.positionSeconds / 60)} min · {saved.percent}%</p>}<Button className="mt-4 w-full" variant="secondary" onClick={openPlayer}>{item.videoUrl ? (saved ? 'Resume playback' : 'Open player') : 'Awaiting video source'}</Button></div>
    </section>
  </div>
}
