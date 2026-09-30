import { useMemo, useSyncExternalStore } from 'react'
import { getToken, getUser, onSessionChange } from './session'

export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined

export function useSession() {
  const token = useSyncExternalStore(onSessionChange, getToken, () => null)
  // The raw string is a stable snapshot; parse it once per change.
  const rawUser = useSyncExternalStore(onSessionChange, readRawUser, () => null)
  const user = useMemo(() => (rawUser ? getUser() : null), [rawUser])
  return { token, user, isAuthenticated: !!token }
}

function readRawUser() {
  try {
    return localStorage.getItem('airdropx.user')
  } catch {
    return null
  }
}

/** Starts Google's OAuth "authorization code" flow. Google sends the user back to /auth/callback. */
export function startGoogleLogin(returnTo = '/app') {
  if (!GOOGLE_CLIENT_ID) throw new Error('Google sign-in is not configured')
  const state = crypto.randomUUID()
  sessionStorage.setItem('oauth.state', state)
  sessionStorage.setItem('oauth.returnTo', returnTo)
  const params = new URLSearchParams({
    client_id: GOOGLE_CLIENT_ID,
    redirect_uri: `${window.location.origin}/auth/callback`,
    response_type: 'code',
    scope: 'openid email profile',
    access_type: 'offline',
    prompt: 'select_account',
    state,
  })
  window.location.assign(`https://accounts.google.com/o/oauth2/v2/auth?${params}`)
}
