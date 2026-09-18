import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, User } from '../types/api'

interface AuthState {
  token: string | null
  user: User | null
  setAuth: (auth: AuthResponse) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (auth) => set({ token: auth.token, user: auth.user }),
      logout: () => set({ token: null, user: null }),
    }),
    { name: 'cinevora-auth' },
  ),
)
