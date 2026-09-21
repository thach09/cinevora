import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getApiError, userDataApi } from '../lib/api'
import { Button, EmptyState, QueryError, Spinner } from '../components/ui'
import { MovieCard, PosterArtwork } from '../components/MovieCard'
import { formatDate } from '../lib/format'
import { useToast } from '../components/ToastProvider'
import { useAuthStore } from '../store/authStore'

export function WatchlistPage() { return <LibraryPage type="watchlist" title="Your watchlist" eyebrow="Saved for later" description="The films you promised yourself you would watch." /> }
export function FavouritesPage() { return <LibraryPage type="favourites" title="Your favourites" eyebrow="The keepers" description="The stories that stayed with you." /> }

function LibraryPage({ type, title, eyebrow, description }: { type: 'watchlist' | 'favourites'; title: string; eyebrow: string; description: string }) {
  const client = useQueryClient()
  const { push } = useToast()
  const activeProfileId = useAuthStore((state) => state.activeProfileId)
  const query = useQuery({ queryKey: [type, activeProfileId], queryFn: type === 'watchlist' ? userDataApi.watchlist : userDataApi.favourites, enabled: Boolean(activeProfileId) })
  const remove = useMutation({ mutationFn: (id: number) => type === 'watchlist' ? userDataApi.removeWatchlist(id) : userDataApi.removeFavourite(id), onSuccess: () => { client.invalidateQueries({ queryKey: [type] }); push(`Removed from your ${type}.`, 'success') }, onError: (error) => push(getApiError(error), 'error') })
  if (query.isLoading) return <Spinner />
  if (query.isError) return <QueryError message={getApiError(query.error)} />
  const entries = query.data || []
  return <div className="space-y-8"><div className="library-heading"><div><p className="eyebrow">{eyebrow}</p><h2 className="section-title mt-1">{title}</h2><p className="mt-2 text-slate-400">{description}</p></div><span className="library-count">{entries.length} {entries.length === 1 ? 'title' : 'titles'}</span></div>{entries.length ? <div className="movie-grid">{entries.map((entry) => <div key={entry.movieId} className="relative"><MovieCard movie={entry} /><button className="remove-chip" onClick={() => remove.mutate(entry.movieId)} aria-label={`Remove ${entry.title}`}>Remove</button></div>)}</div> : <EmptyState title="Nothing here yet" description="When you find a movie worth saving, it will appear in this collection." action={<Link to="/browse"><Button>Browse movies</Button></Link>} />}</div>
}

export function ContinueWatchingPage() {
  const client = useQueryClient()
  const { push } = useToast()
  const activeProfileId = useAuthStore((state) => state.activeProfileId)
  const query = useQuery({ queryKey: ['continue-watching', activeProfileId], queryFn: userDataApi.continueWatching, enabled: Boolean(activeProfileId) })
  const remove = useMutation({ mutationFn: userDataApi.removeContinue, onSuccess: () => { client.invalidateQueries({ queryKey: ['continue-watching'] }); push('Removed from continue watching.', 'success') }, onError: (error) => push(getApiError(error), 'error') })
  if (query.isLoading) return <Spinner />
  if (query.isError) return <QueryError message={getApiError(query.error)} />
  return <div className="space-y-8"><div><p className="eyebrow">Pick up where you left off</p><h2 className="section-title mt-1">Continue watching</h2><p className="mt-2 text-slate-400">A quiet place for the stories already in motion.</p></div>{query.data?.length ? <div className="space-y-4">{query.data.map((entry) => <div key={entry.movieId} className="continue-row"><Link to={`/movies/${entry.movieId}`} className="continue-thumb"><PosterArtwork src={entry.thumbnailUrl} alt={`${entry.title} poster`} title={entry.title} /></Link><div className="min-w-0 flex-1"><Link to={`/movies/${entry.movieId}`} className="truncate text-lg font-semibold text-white hover:text-pink-200">{entry.title}</Link><p className="mt-1 text-sm text-slate-500">Last updated {formatDate(entry.updatedAt)}</p><div className="progress-track mt-4"><span style={{ width: `${entry.percent}%` }} /></div><p className="mt-2 text-xs text-slate-500">{entry.percent}% complete · resume at {Math.floor(entry.positionSeconds / 60)} min</p></div><Button variant="ghost" onClick={() => remove.mutate(entry.movieId)}>Remove</Button></div>)}</div> : <EmptyState title="Nothing in progress" description="Start a movie and save your progress here for an easy return." action={<Link to="/browse"><Button>Find a movie</Button></Link>} />}</div>
}

export function HistoryPage() {
  const { push } = useToast()
  const activeProfileId = useAuthStore((state) => state.activeProfileId)
  const query = useQuery({ queryKey: ['history', activeProfileId], queryFn: userDataApi.history, enabled: Boolean(activeProfileId) })
  const [exporting, setExporting] = useState(false)
  if (query.isLoading) return <Spinner />
  if (query.isError) return <QueryError message={getApiError(query.error)} />
  const download = async () => { setExporting(true); try { const response = await userDataApi.exportHistory(); const url = URL.createObjectURL(response.data); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'watch-history.csv'; anchor.click(); URL.revokeObjectURL(url); push('History exported.', 'success') } catch (error) { push(getApiError(error), 'error') } finally { setExporting(false) } }
  return <div className="space-y-8"><div className="flex flex-wrap items-end justify-between gap-4"><div><p className="eyebrow">A record of your stories</p><h2 className="section-title mt-1">Watch history</h2><p className="mt-2 text-slate-400">A readable record of the movies you have visited.</p></div><Button variant="secondary" onClick={download} disabled={exporting}>{exporting ? 'Exporting...' : 'Export CSV'}</Button></div>{query.data?.length ? <div className="surface overflow-hidden"><div className="history-header"><span>Movie</span><span>Watched on</span><span>Open</span></div>{query.data.map((entry) => <div className="history-row" key={entry.id}><div><p className="font-semibold text-white">{entry.title}</p><p className="mt-1 text-xs text-slate-500">History entry #{entry.id}</p></div><span className="text-sm text-slate-400">{formatDate(entry.watchedAt)}</span><Link className="text-sm font-semibold text-pink-300 hover:text-pink-200" to={`/movies/${entry.movieId}`}>View</Link></div>)}</div> : <EmptyState title="Your history is empty" description="Movies you mark as watched will show up here." action={<Link to="/browse"><Button>Browse movies</Button></Link>} />}</div>
}
