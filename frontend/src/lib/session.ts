import type { SessionUser } from './types'

const TOKEN_KEY = 'airdropx.token'
const USER_KEY = 'airdropx.user'
const EXP_KEY = 'airdropx.expires'

type Listener = () => void
const listeners = new Set<Listener>()

function safeGet(key: string) {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

export function getToken(): string | null {
  const token = safeGet(TOKEN_KEY)
  const exp = Number(safeGet(EXP_KEY) ?? 0)
  // expires_at may be seconds or milliseconds; treat both
  const expMs = exp > 1e12 ? exp : exp * 1000
  if (token && exp && expMs < Date.now()) {
    clearSession()
    return null
  }
  return token
}

export function getUser(): SessionUser | null {
  const raw = safeGet(USER_KEY)
  try {
    return raw ? (JSON.parse(raw) as SessionUser) : null
  } catch {
    return null
  }
}

export function saveSession(token: string, user: SessionUser, expiresAt?: string | number | null) {
  try {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    if (expiresAt) localStorage.setItem(EXP_KEY, String(expiresAt))
    else localStorage.removeItem(EXP_KEY)
  } catch {
    /* storage unavailable: the session lasts for this tab only */
  }
  listeners.forEach((l) => l())
}

export function clearSession() {
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(EXP_KEY)
  } catch {
    /* ignore */
  }
  listeners.forEach((l) => l())
}

export function onSessionChange(listener: Listener) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

/** Changes fields of the stored user (for example the display name after a profile edit) without touching the token. */
export function updateSessionUser(patch: Partial<SessionUser>) {
  const current = getUser()
  if (!current) return
  try {
    localStorage.setItem(USER_KEY, JSON.stringify({ ...current, ...patch }))
  } catch {
    /* storage unavailable */
  }
  listeners.forEach((l) => l())
}
