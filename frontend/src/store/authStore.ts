import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, User } from '../types/api'

interface AuthState {
  token: string | null
  refreshToken: string | null
  user: User | null
  activeProfileId: number | null
  setAuth: (auth: AuthResponse) => void
  setActiveProfile: (profileId: number) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      user: null,
      activeProfileId: null,
      setAuth: (auth) => set({ token: auth.token, refreshToken: auth.refreshToken, user: auth.user }),
      setActiveProfile: (activeProfileId) => set({ activeProfileId }),
      logout: () => set({ token: null, refreshToken: null, user: null, activeProfileId: null }),
    }),
    { name: 'cinevora-auth' },
  ),
)
