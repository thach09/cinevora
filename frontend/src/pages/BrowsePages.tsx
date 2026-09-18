import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { categoryApi, getApiError, movieApi } from '../lib/api'
import { MovieCard, MovieGrid } from '../components/MovieCard'
import { Button, EmptyState, QueryError, Spinner } from '../components/ui'
import { formatNumber } from '../lib/format'
import { useToast } from '../components/ToastProvider'

export function BrowseMoviePage() {
  const [page, setPage] = useState(0)
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoryApi.list })
  const movies = useQuery({ queryKey: ['movies', 'browse', page], queryFn: () => movieApi.list({ page, size: 12, sort: 'popularity', direction: 'desc' }) })
  const trending = useQuery({ queryKey: ['movies', 'trending'], queryFn: () => movieApi.trending({ size: 5 }) })
  if (movies.isLoading) return <Spinner label="Curating your cinema" />
  if (movies.isError) return <QueryError message={getApiError(movies.error)} />
  const featured = trending.data?.content[0] || movies.data?.content[0]
  return <div className="space-y-12"><section className="hero-banner"><div className="hero-copy"><p className="eyebrow text-pink-200">Tonight’s recommendation</p><h2>{featured?.title || 'Find a film for every feeling.'}</h2><p className="hero-description">{featured?.description || 'Explore a thoughtful catalogue of movies, hand-picked by your own curiosity.'}</p><div className="mt-7 flex flex-wrap gap-3"><Button onClick={() => featured && window.location.assign(`/movies/${featured.id}`)}>▶ Watch details</Button><Button variant="secondary" onClick={() => document.getElementById('catalogue')?.scrollIntoView({ behavior: 'smooth' })}>Explore catalogue</Button></div></div>{featured && <div className="hero-meta"><span className="hero-score">★ {featured.rating.toFixed(1)}</span><span>{featured.releaseYear}</span><span>{featured.categoryName}</span><span>{formatNumber(featured.views)} views</span></div>}</section><section id="catalogue" className="space-y-5"><div className="section-heading"><div><p className="eyebrow">Browse by mood</p><h2 className="section-title">A little something for everyone</h2></div><Link className="text-sm font-semibold text-pink-300 hover:text-pink-200" to="/search">Advanced search →</Link></div><div className="category-pills"><Link to="/browse" className="category-pill category-pill-active">All titles</Link>{categories.data?.map((category) => <Link key={category.id} to={`/search?categoryId=${category.id}`} className="category-pill">{category.name}</Link>)}</div>{movies.data?.content.length ? <MovieGrid movies={movies.data.content} /> : <EmptyState title="The catalogue is quiet" description="There are no active movies to show yet." />}</section><Pagination page={page} totalPages={movies.data?.totalPages || 0} onChange={setPage} /></div>
}

export function SearchPage() {
  const [searchParams] = useSearchParams(); const navigate = useNavigate(); const [q, setQ] = useState(searchParams.get('q') || ''); const [categoryId, setCategoryId] = useState(searchParams.get('categoryId') || ''); const [minRating, setMinRating] = useState(''); const [sort, setSort] = useState('popularity'); const [page, setPage] = useState(0)
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoryApi.list })
  const query = { q: q || undefined, categoryId: categoryId ? Number(categoryId) : undefined, minRating: minRating ? Number(minRating) : undefined, page, size: 12, sort, direction: 'desc' as const }
  const movies = useQuery({ queryKey: ['movies', 'search', query], queryFn: () => movieApi.list(query) })
  const submit = (event: FormEvent) => { event.preventDefault(); setPage(0); navigate(`/search${q ? `?q=${encodeURIComponent(q)}` : ''}`, { replace: true }) }
  return <div className="space-y-8"><div><p className="eyebrow">The whole catalogue</p><h2 className="section-title mt-1">Search and discover</h2><p className="mt-2 text-slate-400">Find a new favourite by title, category, rating, or just a feeling.</p></div><form className="search-panel" onSubmit={submit}><div className="search-input-wrap"><span>⌕</span><input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search by title, director, or actor" /></div><select className="input filter-input" value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0) }}><option value="">All categories</option>{categories.data?.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select><select className="input filter-input" value={minRating} onChange={(event) => { setMinRating(event.target.value); setPage(0) }}><option value="">Any rating</option><option value="7">7+ rating</option><option value="8">8+ rating</option><option value="9">9+ rating</option></select><select className="input filter-input" value={sort} onChange={(event) => { setSort(event.target.value); setPage(0) }}><option value="popularity">Most popular</option><option value="rating">Top rated</option><option value="releaseYear">Newest</option><option value="title">Title A–Z</option></select><Button type="submit">Search</Button></form>{movies.isLoading ? <Spinner /> : movies.isError ? <QueryError message={getApiError(movies.error)} /> : movies.data?.content.length ? <><MovieGrid movies={movies.data.content} /><Pagination page={page} totalPages={movies.data.totalPages} onChange={setPage} /></> : <EmptyState title="No matches yet" description="Try a broader search or remove one of the filters." />}</div>
}

function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages < 2) return null
  return <div className="flex items-center justify-center gap-4 text-sm"><Button variant="ghost" disabled={page === 0} onClick={() => onChange(page - 1)}>← Previous</Button><span className="text-slate-400">Page <strong className="text-white">{page + 1}</strong> of {totalPages}</span><Button variant="ghost" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next →</Button></div>
}
