import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { lazy, Suspense, type ReactNode } from 'react'
import { useAuthStore } from './store/authStore'
import { AppLayout } from './components/Layout'
import { Spinner } from './components/ui'

const LoginPage = lazy(() => import('./pages/AuthPages').then((module) => ({ default: module.LoginPage })))
const RegisterPage = lazy(() => import('./pages/AuthPages').then((module) => ({ default: module.RegisterPage })))
const ForgotPasswordPage = lazy(() => import('./pages/AuthPages').then((module) => ({ default: module.ForgotPasswordPage })))
const ResetPasswordPage = lazy(() => import('./pages/AuthPages').then((module) => ({ default: module.ResetPasswordPage })))
const VerifyEmailPage = lazy(() => import('./pages/AuthPages').then((module) => ({ default: module.VerifyEmailPage })))
const BrowseMoviePage = lazy(() => import('./pages/BrowsePages').then((module) => ({ default: module.BrowseMoviePage })))
const SearchPage = lazy(() => import('./pages/BrowsePages').then((module) => ({ default: module.SearchPage })))
const MovieDetailPage = lazy(() => import('./pages/MovieDetailPage').then((module) => ({ default: module.MovieDetailPage })))
const WatchlistPage = lazy(() => import('./pages/LibraryPages').then((module) => ({ default: module.WatchlistPage })))
const FavouritesPage = lazy(() => import('./pages/LibraryPages').then((module) => ({ default: module.FavouritesPage })))
const ContinueWatchingPage = lazy(() => import('./pages/LibraryPages').then((module) => ({ default: module.ContinueWatchingPage })))
const HistoryPage = lazy(() => import('./pages/LibraryPages').then((module) => ({ default: module.HistoryPage })))
const AdminDashboardPage = lazy(() => import('./pages/AdminPages').then((module) => ({ default: module.AdminDashboardPage })))
const AdminMoviePage = lazy(() => import('./pages/AdminPages').then((module) => ({ default: module.AdminMoviePage })))
const AdminCategoryPage = lazy(() => import('./pages/AdminPages').then((module) => ({ default: module.AdminCategoryPage })))
const AdminArchivePage = lazy(() => import('./pages/AdminPages').then((module) => ({ default: module.AdminArchivePage })))
const AdminMediaPage = lazy(() => import('./pages/AdminPages').then((module) => ({ default: module.AdminMediaPage })))
const AdminUserPage = lazy(() => import('./pages/AdminPages').then((module) => ({ default: module.AdminUserPage })))
const AdminStatisticsPage = lazy(() => import('./pages/AdminPages').then((module) => ({ default: module.AdminStatisticsPage })))
const AccountPage = lazy(() => import('./pages/AccountPage').then((module) => ({ default: module.AccountPage })))

export function AppRoutes() {
  return <Suspense fallback={<Spinner label="Loading Cinevora" />}><Routes>
    <Route path="/login" element={<PublicOnly><LoginPage /></PublicOnly>} />
    <Route path="/register" element={<PublicOnly><RegisterPage /></PublicOnly>} />
    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
    <Route path="/reset-password" element={<ResetPasswordPage />} />
    <Route path="/verify-email" element={<VerifyEmailPage />} />
    <Route element={<ProtectedRoute />}>
      <Route element={<AppLayout />}>
        <Route index element={<HomeRedirect />} />
        <Route path="browse" element={<BrowseMoviePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="movies/:id" element={<MovieDetailPage />} />
        <Route path="watchlist" element={<WatchlistPage />} />
        <Route path="favourites" element={<FavouritesPage />} />
        <Route path="continue-watching" element={<ContinueWatchingPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="account" element={<AccountPage />} />
        <Route path="admin" element={<AdminRoute />}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="movies" element={<AdminMoviePage />} />
          <Route path="categories" element={<AdminCategoryPage />} />
          <Route path="archive" element={<AdminArchivePage />} />
          <Route path="media" element={<AdminMediaPage />} />
          <Route path="users" element={<AdminUserPage />} />
          <Route path="statistics" element={<AdminStatisticsPage />} />
        </Route>
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></Suspense>
}

function ProtectedRoute() { const authenticated = useAuthStore((state) => Boolean(state.token && state.user)); return authenticated ? <Outlet /> : <Navigate to="/login" replace state={{ message: 'Sign in to continue.' }} /> }
function PublicOnly({ children }: { children: ReactNode }) { const user = useAuthStore((state) => state.user); return user ? <Navigate to={user.role === 'ADMIN' ? '/admin' : '/browse'} replace /> : children }
function AdminRoute() { const role = useAuthStore((state) => state.user?.role); return role === 'ADMIN' ? <Outlet /> : <Navigate to="/browse" replace /> }
function HomeRedirect() { const role = useAuthStore((state) => state.user?.role); return <Navigate to={role === 'ADMIN' ? '/admin' : '/browse'} replace /> }
