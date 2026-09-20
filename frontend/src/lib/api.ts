import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import type {
  ApiResponse, AuthResponse, Category, ContinueEntry, HistoryEntry, LoginRequest,
  Movie, MovieQuery, MovieRef, PageResponse, RegisterRequest, Statistics,
} from '../types/api'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) useAuthStore.getState().logout()
    return Promise.reject(error)
  },
)

const dataOf = async <T>(request: Promise<{ data: ApiResponse<T> }>) => (await request).data.data

export const authApi = {
  login: (payload: LoginRequest) => dataOf<AuthResponse>(api.post('/auth/login', payload)),
  register: (payload: RegisterRequest) => dataOf<AuthResponse>(api.post('/auth/register', payload)),
}

export const categoryApi = {
  list: () => dataOf<Category[]>(api.get('/categories')),
  create: (payload: { name: string; description?: string }) => dataOf<Category>(api.post('/categories', payload)),
  update: (id: number, payload: { name: string; description?: string }) => dataOf<Category>(api.put(`/categories/${id}`, payload)),
  remove: (id: number) => dataOf<void>(api.delete(`/categories/${id}`)),
  restore: (id: number) => dataOf<Category>(api.patch(`/categories/${id}/restore`)),
}

export const movieApi = {
  list: (params: MovieQuery = {}) => dataOf<PageResponse<Movie>>(api.get('/movies', { params })),
  trending: (params: { page?: number; size?: number } = {}) => dataOf<PageResponse<Movie>>(api.get('/movies/trending', { params })),
  get: (id: number) => dataOf<Movie>(api.get(`/movies/${id}`)),
  create: (payload: MoviePayload) => dataOf<Movie>(api.post('/movies', payload)),
  update: (id: number, payload: MoviePayload) => dataOf<Movie>(api.put(`/movies/${id}`, payload)),
  remove: (id: number) => dataOf<void>(api.delete(`/movies/${id}`)),
  restore: (id: number) => dataOf<Movie>(api.patch(`/movies/${id}/restore`)),
}

export interface MoviePayload {
  title: string
  categoryId: number
  director: string
  actors: string
  releaseYear: number
  rating: number
  durationMinutes?: number
  videoUrl?: string
  thumbnailUrl?: string
  description?: string
}

export const userDataApi = {
  watchlist: () => dataOf<MovieRef[]>(api.get('/users/me/watchlist')),
  addWatchlist: (movieId: number) => dataOf<void>(api.post(`/users/me/watchlist/${movieId}`)),
  removeWatchlist: (movieId: number) => dataOf<void>(api.delete(`/users/me/watchlist/${movieId}`)),
  favourites: () => dataOf<MovieRef[]>(api.get('/users/me/favourites')),
  addFavourite: (movieId: number) => dataOf<void>(api.post(`/users/me/favourites/${movieId}`)),
  removeFavourite: (movieId: number) => dataOf<void>(api.delete(`/users/me/favourites/${movieId}`)),
  history: () => dataOf<HistoryEntry[]>(api.get('/users/me/history')),
  addHistory: (movieId: number) => dataOf<void>(api.post(`/users/me/history/${movieId}`)),
  continueWatching: () => dataOf<ContinueEntry[]>(api.get('/users/me/continue-watching')),
  updateProgress: (movieId: number, positionSeconds: number, durationSeconds?: number) => dataOf<void>(api.put('/users/me/continue-watching', {
    movieId,
    positionSeconds: Math.max(0, Math.floor(positionSeconds)),
    durationSeconds: durationSeconds && durationSeconds > 0 ? Math.floor(durationSeconds) : undefined,
    percent: durationSeconds && durationSeconds > 0 ? Math.round((positionSeconds / durationSeconds) * 100) : undefined,
  })),
  removeContinue: (movieId: number) => dataOf<void>(api.delete(`/users/me/continue-watching/${movieId}`)),
  exportHistory: () => api.get('/users/me/history/export', { responseType: 'blob' }),
}

export const mediaApi = {
  uploadPoster: (movieId: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return dataOf<Movie>(api.post(`/media/movies/${movieId}/poster`, form, { headers: { 'Content-Type': 'multipart/form-data' } }))
  },
  removePoster: (movieId: number) => dataOf<Movie>(api.delete(`/media/movies/${movieId}/poster`)),
}

export const statisticsApi = { get: () => dataOf<Statistics>(api.get('/statistics')) }

export const getApiError = (error: unknown) => {
  if (axios.isAxiosError(error)) return error.response?.data?.message || error.message
  return 'Something went wrong. Please try again.'
}
