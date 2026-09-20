import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { Movie, MovieRef } from '../types/api'
import { formatNumber } from '../lib/format'

type CardMovie = Movie | MovieRef
const isMovie = (movie: CardMovie): movie is Movie => 'categoryName' in movie

export function MovieCard({ movie, compact = false }: { movie: CardMovie; compact?: boolean }) {
  const id = isMovie(movie) ? movie.id : movie.movieId
  const title = movie.title
  return (
    <Link to={`/movies/${id}`} className={`movie-card group ${compact ? 'movie-card-compact' : ''}`}>
      <div className="movie-poster">
        <PosterArtwork src={movie.thumbnailUrl} alt={`${title} poster`} title={title} />
        <div className="poster-overlay"><span className="play-chip">▶</span></div>
        {isMovie(movie) && <span className="poster-rating">★ {movie.rating.toFixed(1)}</span>}
      </div>
      <div className="mt-3 min-w-0">
        <h3 className="truncate text-sm font-semibold text-white group-hover:text-pink-200">{title}</h3>
        <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
          {isMovie(movie) ? <><span>{movie.releaseYear}</span><span>•</span><span>{movie.categoryName}</span><span>•</span><span>{formatNumber(movie.views)} views</span></> : <span>Saved to your library</span>}
        </div>
      </div>
    </Link>
  )
}

export function PosterArtwork({ src, alt, title, className = '' }: { src?: string | null; alt: string; title: string; className?: string }) {
  const [status, setStatus] = useState<'loading' | 'loaded' | 'error'>(src ? 'loading' : 'error')
  useEffect(() => setStatus(src ? 'loading' : 'error'), [src])
  const showImage = Boolean(src) && status !== 'error'

  return (
    <>
      {!showImage && <div className={`poster-fallback ${className}`} aria-hidden="true"><span>{title.slice(0, 1).toUpperCase()}</span></div>}
      {showImage && <img className={className} src={src || undefined} alt={alt} loading="lazy" onLoad={() => setStatus('loaded')} onError={() => setStatus('error')} />}
    </>
  )
}

export function MovieGrid({ movies }: { movies: CardMovie[] }) {
  return <div className="movie-grid">{movies.map((movie) => <MovieCard key={isMovie(movie) ? movie.id : movie.movieId} movie={movie} />)}</div>
}
