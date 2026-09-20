import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import type {
  ApiResponse, AuthResponse, Category, ContinueEntry, HistoryEntry, LoginRequest,
  Movie, MovieQuery, MovieRef, PageResponse, RegisterRequest, Statistics, User, Profile, AccountSession,
} from '../types/api'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  const profileId = useAuthStore.getState().activeProfileId
  if (profileId) config.headers['X-Profile-Id'] = String(profileId)
  return config
})

let refreshPromise: Promise<string> | null = null
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const request = error.config as typeof error.config & { _retry?: boolean } | undefined
    const isRefresh = request?.url?.includes('/auth/refresh') || request?.url?.includes('/auth/login')
    if (error.response?.status !== 401 || !request || request._retry || isRefresh || !useAuthStore.getState().refreshToken) {
      if (error.response?.status === 401 && isRefresh) useAuthStore.getState().logout()
      return Promise.reject(error)
    }
    request._retry = true
    refreshPromise ||= authApi.refresh(useAuthStore.getState().refreshToken!).then((auth) => { useAuthStore.getState().setAuth(auth); return auth.token }).finally(() => { refreshPromise = null })
    return refreshPromise.then((token) => { request.headers.Authorization = `Bearer ${token}`; return api(request) }).catch((refreshError) => { useAuthStore.getState().logout(); return Promise.reject(refreshError) })
  },
)

const dataOf = async <T>(request: Promise<{ data: ApiResponse<T> }>) => (await request).data.data

export const authApi = {
  login: (payload: LoginRequest) => dataOf<AuthResponse>(api.post('/auth/login', payload)),
  register: (payload: RegisterRequest) => dataOf<AuthResponse>(api.post('/auth/register', payload)),
  refresh: (refreshToken: string) => dataOf<AuthResponse>(api.post('/auth/refresh', { refreshToken })),
  logout: (refreshToken: string | null) => dataOf<void>(api.post('/auth/logout', { refreshToken })),
  forgotPassword: (email: string) => dataOf<{ message: string; developmentToken?: string | null }>(api.post('/auth/forgot-password', { email })),
  resetPassword: (token: string, newPassword: string) => dataOf<void>(api.post('/auth/reset-password', { token, newPassword })),
  verifyEmail: (token: string) => dataOf<void>(api.post('/auth/verify-email', { token })),
}

export const accountApi = {
  me: () => dataOf<User>(api.get('/users/me')),
  update: (payload: { fullName: string; email: string }) => dataOf<User>(api.put('/users/me', payload)),
  changePassword: (payload: { currentPassword: string; newPassword: string }) => dataOf<void>(api.put('/users/me/password', payload)),
  sessions: () => dataOf<AccountSession[]>(api.get('/users/me/sessions')),
  revokeSession: (id: number) => dataOf<void>(api.delete(`/users/me/sessions/${id}`)),
}

export const profileApi = {
  list: () => dataOf<Profile[]>(api.get('/users/me/profiles')),
  create: (payload: { name: string; avatarUrl?: string }) => dataOf<Profile>(api.post('/users/me/profiles', payload)),
  update: (id: number, payload: { name: string; avatarUrl?: string }) => dataOf<Profile>(api.put(`/users/me/profiles/${id}`, payload)),
  remove: (id: number) => dataOf<void>(api.delete(`/users/me/profiles/${id}`)),
  select: (id: number) => dataOf<Profile>(api.post(`/users/me/profiles/${id}/select`)),
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
