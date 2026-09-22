import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  if (env.VERCEL && (!env.VITE_API_URL?.startsWith('https://') || /localhost|127\.0\.0\.1/.test(env.VITE_API_URL))) {
    throw new Error('Vercel builds require VITE_API_URL pointing to the deployed HTTPS API')
  }
  return {
  plugins: [react()],
  server: {
    port: 5173,
    host: 'localhost',
  },
  }
})
