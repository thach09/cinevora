import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { categoryApi, getApiError, movieApi } from '../lib/api'
import { MovieGrid } from '../components/MovieCard'
import { Button, EmptyState, QueryError, Spinner } from '../components/ui'
import { formatNumber } from '../lib/format'

export function BrowseMoviePage() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoryApi.list })
  const movies = useQuery({ queryKey: ['movies', 'browse', page], queryFn: () => movieApi.list({ page, size: 12, sort: 'popularity', direction: 'desc' }) })
  const trending = useQuery({ queryKey: ['movies', 'trending'], queryFn: () => movieApi.trending({ size: 5 }) })

  if (movies.isLoading) return <Spinner label="Curating your cinema" />
  if (movies.isError) return <QueryError message={getApiError(movies.error)} />

  const featured = trending.data?.content[0] || movies.data?.content[0]
  return (
    <div className="space-y-12">
      <section className="hero-banner">
        <HeroArtwork movie={featured} />
        <div className="hero-copy">
          <p className="eyebrow text-pink-200">Tonight's recommendation</p>
          <h2>{featured?.title || 'Find a film for every feeling.'}</h2>
          <p className="hero-description">{featured?.description || 'Explore a thoughtful catalogue of movies, hand-picked by your own curiosity.'}</p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Button onClick={() => featured && navigate(`/movies/${featured.id}`)}>Play details</Button>
            <Button variant="secondary" onClick={() => document.getElementById('catalogue')?.scrollIntoView({ behavior: 'smooth' })}>Explore catalogue</Button>
          </div>
        </div>
        {featured && <div className="hero-meta"><span className="hero-score">★ {featured.rating.toFixed(1)}</span><span>{featured.releaseYear}</span><span>{featured.categoryName}</span>{featured.durationMinutes && <span>{featured.durationMinutes} min</span>}<span>{formatNumber(featured.views)} views</span></div>}
      </section>

      <section id="catalogue" className="space-y-6">
        <div className="section-heading">
          <div><p className="eyebrow">Popular in Cinevora</p><h2 className="section-title">A little something for everyone</h2></div>
          <Link className="text-sm font-semibold text-pink-300 transition hover:text-pink-200" to="/search">Advanced search <span aria-hidden="true">-&gt;</span></Link>
        </div>
        <div className="category-pills" aria-label="Browse categories">
          <Link to="/browse" className="category-pill category-pill-active">All titles</Link>
          {categories.data?.map((category) => <Link key={category.id} to={`/search?categoryId=${category.id}`} className="category-pill">{category.name}</Link>)}
        </div>
        {movies.data?.content.length ? <MovieGrid movies={movies.data.content} /> : <EmptyState title="The catalogue is quiet" description="There are no active movies to show yet." />}
      </section>
      <Pagination page={page} totalPages={movies.data?.totalPages || 0} onChange={setPage} />
    </div>
  )
}

function HeroArtwork({ movie }: { movie?: { title: string; thumbnailUrl: string | null } }) {
  const [failed, setFailed] = useState(!movie?.thumbnailUrl)
  useEffect(() => setFailed(!movie?.thumbnailUrl), [movie?.thumbnailUrl])
  return <div className="hero-media" aria-hidden="true"><div className="hero-art-fallback"><span>{movie?.title.slice(0, 1).toUpperCase() || 'C'}</span></div>{movie?.thumbnailUrl && !failed && <img src={movie.thumbnailUrl} alt="" onError={() => setFailed(true)} />}</div>
}

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [q, setQ] = useState(searchParams.get('q') || '')
  const [categoryId, setCategoryId] = useState(searchParams.get('categoryId') || '')
  const [minRating, setMinRating] = useState('')
  const [sort, setSort] = useState('popularity')
  const [page, setPage] = useState(0)
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoryApi.list })
  const query = { q: q || undefined, categoryId: categoryId ? Number(categoryId) : undefined, minRating: minRating ? Number(minRating) : undefined, page, size: 12, sort, direction: 'desc' as const }
  const movies = useQuery({ queryKey: ['movies', 'search', query], queryFn: () => movieApi.list(query) })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    setPage(0)
    const params = new URLSearchParams()
    if (q) params.set('q', q)
    if (categoryId) params.set('categoryId', categoryId)
    navigate(`/search${params.toString() ? `?${params.toString()}` : ''}`, { replace: true })
  }

  return (
    <div className="space-y-8">
      <div><p className="eyebrow">The whole catalogue</p><h2 className="section-title mt-1">Search and discover</h2><p className="mt-2 max-w-2xl text-slate-400">Find a new favourite by title, category, rating, or just a feeling.</p></div>
      <form className="search-panel" onSubmit={submit}>
        <div className="search-input-wrap"><span aria-hidden="true">⌕</span><input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search by title, director, or actor" aria-label="Search movies" /></div>
        <select className="input filter-input" value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0) }} aria-label="Filter by category"><option value="">All categories</option>{categories.data?.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select>
        <select className="input filter-input" value={minRating} onChange={(event) => { setMinRating(event.target.value); setPage(0) }} aria-label="Filter by rating"><option value="">Any rating</option><option value="7">7+ rating</option><option value="8">8+ rating</option><option value="9">9+ rating</option></select>
        <select className="input filter-input" value={sort} onChange={(event) => { setSort(event.target.value); setPage(0) }} aria-label="Sort results"><option value="popularity">Most popular</option><option value="rating">Top rated</option><option value="releaseYear">Newest</option><option value="title">Title A-Z</option></select>
        <Button type="submit">Search</Button>
      </form>
      {movies.isLoading ? <Spinner label="Searching the catalogue" /> : movies.isError ? <QueryError message={getApiError(movies.error)} /> : movies.data?.content.length ? <><MovieGrid movies={movies.data.content} /><Pagination page={page} totalPages={movies.data.totalPages} onChange={setPage} /></> : <EmptyState title="No matches yet" description="Try a broader search or remove one of the filters." />}
    </div>
  )
}

function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages < 2) return null
  return <div className="pagination"><Button variant="ghost" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</Button><span className="text-slate-400">Page <strong className="text-white">{page + 1}</strong> of {totalPages}</span><Button variant="ghost" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next</Button></div>
}
