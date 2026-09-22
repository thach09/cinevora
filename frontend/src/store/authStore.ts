import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, User } from '../types/api'

interface AuthState {
  token: string | null
  refreshToken: string | null
  user: User | null
  activeProfileId: number | null
  activeProfileUserId: number | null
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
      activeProfileUserId: null,
      setAuth: (auth) => set((state) => {
        const sameUser = state.user?.id === auth.user.id
        return {
          token: auth.token,
          refreshToken: auth.refreshToken,
          user: auth.user,
          activeProfileId: sameUser ? state.activeProfileId : null,
          activeProfileUserId: sameUser ? state.activeProfileUserId ?? null : null,
        }
      }),
      setActiveProfile: (activeProfileId) => set((state) => ({ activeProfileId, activeProfileUserId: state.user?.id ?? null })),
      logout: () => set({ token: null, refreshToken: null, user: null, activeProfileId: null, activeProfileUserId: null }),
    }),
    { name: 'cinevora-auth' },
  ),
)
