import { create } from 'zustand'
import type { AuthResponse, User } from '../types/api'

// Remove the previous persisted credential store during the migration.
try { localStorage.removeItem('cinevora-auth'); sessionStorage.removeItem('cinevora-auth') } catch { /* storage can be disabled */ }
const ACTIVE_PROFILE_KEY = 'cinevora-active-profile'
function storedProfile(userId: number) {
  try {
    const value = JSON.parse(sessionStorage.getItem(ACTIVE_PROFILE_KEY) || 'null') as { userId?: number; profileId?: number } | null
    return value?.userId === userId && Number.isInteger(value.profileId) ? value.profileId! : null
  } catch { return null }
}
interface AuthState {
  token: string | null
  user: User | null
  activeProfileId: number | null
  activeProfileUserId: number | null
  ready: boolean
  setReady: () => void
  setAuth: (auth: AuthResponse) => void
  setActiveProfile: (id: number) => void
  logout: () => void
}
export const useAuthStore = create<AuthState>()((set) => ({
  token: null, user: null, activeProfileId: null, activeProfileUserId: null, ready: false,
  setReady: () => set({ ready: true }),
  setAuth: (auth) => set((state) => ({
    token: auth.token, user: auth.user, ready: true,
    activeProfileId: state.user?.id === auth.user.id ? state.activeProfileId : storedProfile(auth.user.id),
    activeProfileUserId: state.user?.id === auth.user.id ? state.activeProfileUserId : (storedProfile(auth.user.id) ? auth.user.id : null),
  })),
  setActiveProfile: (id) => set((state) => { if (state.user) { try { sessionStorage.setItem(ACTIVE_PROFILE_KEY, JSON.stringify({ userId: state.user.id, profileId: id })) } catch { /* storage can be disabled */ } } return { activeProfileId: id, activeProfileUserId: state.user?.id ?? null } }),
  logout: () => { try { sessionStorage.removeItem(ACTIVE_PROFILE_KEY) } catch { /* storage can be disabled */ } return set({ token: null, user: null, activeProfileId: null, activeProfileUserId: null, ready: true }) },
}))
