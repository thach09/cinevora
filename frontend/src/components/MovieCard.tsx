import { Link } from 'react-router-dom'
import type { Movie, MovieRef } from '../types/api'
import { formatNumber } from '../lib/format'

type CardMovie = Movie | MovieRef
const isMovie = (movie: CardMovie): movie is Movie => 'categoryName' in movie

export function MovieCard({ movie, compact = false }: { movie: CardMovie; compact?: boolean }) {
  const id = isMovie(movie) ? movie.id : movie.movieId
  const title = movie.title
  const image = movie.thumbnailUrl
  return (
    <Link to={`/movies/${id}`} className={`movie-card group ${compact ? 'movie-card-compact' : ''}`}>
      <div className="movie-poster">
        {image ? <img src={image} alt="" loading="lazy" onError={(event) => { event.currentTarget.style.display = 'none' }} /> : null}
        <div className="poster-fallback"><span>{title.slice(0, 1).toUpperCase()}</span></div>
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

export function MovieGrid({ movies }: { movies: Movie[] }) {
  return <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">{movies.map((movie) => <MovieCard key={movie.id} movie={movie} />)}</div>
}
