import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuthStore } from './store/authStore'
import { AppLayout } from './components/Layout'
import { ForgotPasswordPage, LoginPage, RegisterPage, ResetPasswordPage, VerifyEmailPage } from './pages/AuthPages'
import { BrowseMoviePage, SearchPage } from './pages/BrowsePages'
import { MovieDetailPage } from './pages/MovieDetailPage'
import { ContinueWatchingPage, FavouritesPage, HistoryPage, WatchlistPage } from './pages/LibraryPages'
import { AdminArchivePage, AdminCategoryPage, AdminDashboardPage, AdminMoviePage, AdminStatisticsPage, AdminUserPage } from './pages/AdminPages'
import { AccountPage } from './pages/AccountPage'

export function AppRoutes() {
  return <Routes>
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
          <Route path="users" element={<AdminUserPage />} />
          <Route path="statistics" element={<AdminStatisticsPage />} />
        </Route>
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
}

function ProtectedRoute() { const authenticated = useAuthStore((state) => Boolean(state.token && state.user)); return authenticated ? <Outlet /> : <Navigate to="/login" replace state={{ message: 'Sign in to continue.' }} /> }
function PublicOnly({ children }: { children: ReactNode }) { const user = useAuthStore((state) => state.user); return user ? <Navigate to={user.role === 'ADMIN' ? '/admin' : '/browse'} replace /> : children }
function AdminRoute() { const role = useAuthStore((state) => state.user?.role); return role === 'ADMIN' ? <Outlet /> : <Navigate to="/browse" replace /> }
function HomeRedirect() { const role = useAuthStore((state) => state.user?.role); return <Navigate to={role === 'ADMIN' ? '/admin' : '/browse'} replace /> }
