export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
}

export interface User {
  id: number
  username: string
  email: string
  fullName: string
  role: 'ADMIN' | 'CUSTOMER'
  active: boolean
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresInSeconds: number
  user: User
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  fullName: string
}

export interface Category {
  id: number
  name: string
  description: string | null
  active: boolean
}

export interface Movie {
  id: number
  categoryId: number
  categoryName: string
  title: string
  director: string
  actors: string
  releaseYear: number
  rating: number
  views: number
  favouritesCount: number
  durationMinutes: number | null
  videoUrl: string | null
  thumbnailUrl: string | null
  description: string | null
  active: boolean
}

export interface MovieQuery {
  q?: string
  categoryId?: number
  minYear?: number
  maxYear?: number
  minRating?: number
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
}

export interface MovieRef {
  movieId: number
  title: string
  thumbnailUrl: string | null
  addedAt: string
}

export interface HistoryEntry {
  id: number
  movieId: number
  title: string
  watchedAt: string
}

export interface ContinueEntry {
  movieId: number
  title: string
  thumbnailUrl: string | null
  percent: number
  positionSeconds: number
  durationSeconds: number | null
  updatedAt: string
}

export interface Statistics {
  totalUsers: number
  activeUsers: number
  totalMovies: number
  activeMovies: number
  totalCategories: number
  totalViews: number
  totalFavourites: number
  topMovies: Movie[]
}
