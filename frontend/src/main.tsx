import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppRoutes } from './routes'
import { ToastProvider } from './components/ToastProvider'
import './styles.css'
import { authApi } from './lib/api'
import { useAuthStore } from './store/authStore'
import { I18nProvider, useI18n } from './lib/i18n'

// Runs once outside StrictMode effects; no private routes mount until restored.
const restoration = authApi.restore().then(auth => auth ? useAuthStore.getState().setAuth(auth) : useAuthStore.getState().logout())
  .catch(() => useAuthStore.getState().logout())
  .finally(() => useAuthStore.getState().setReady())
function SessionBoundary() {
  const ready = useAuthStore(state => state.ready)
  const { t } = useI18n()
  return ready ? <AppRoutes /> : <p role="status">{t('app.loading')}</p>
}
void restoration


const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false } } })

ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><QueryClientProvider client={queryClient}><I18nProvider><BrowserRouter><ToastProvider><SessionBoundary /></ToastProvider></BrowserRouter></I18nProvider></QueryClientProvider></React.StrictMode>)
