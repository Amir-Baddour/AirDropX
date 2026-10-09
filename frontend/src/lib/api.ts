import { clearSession, getToken } from './session'

/** Error thrown for any non-2xx response. `errors` holds the per-row/per-task list sent with 422. */
export class ApiError extends Error {
  status: number
  errors: string[]
  code?: string
  reason?: string | null
  constructor(status: number, message: string, errors: string[] = [], code?: string, reason?: string | null) {
    super(message)
    this.status = status
    this.errors = errors
    this.code = code
    this.reason = reason
  }
  get isAuth() {
    return this.code === 'AUTH'
  }
}

// The auth middleware halts with a plain-text code (not JSON) when the token is missing or bad.
const AUTH_CODES = ['AUTH_REQUIRED', 'INVALID_TOKEN', 'EXPIRED_TOKEN']

type Body = { success?: boolean; message?: string; data?: unknown; errors?: string[]; code?: string; reason?: string | null }

function parseJson(text: string): Body | null {
  if (!text) return null
  try {
    return JSON.parse(text) as Body
  } catch {
    return null
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

async function request<T>(method: Method, path: string, body?: unknown, auth = true): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = auth ? getToken() : null
  if (token) headers.Authorization = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new ApiError(0, 'Network error: check your connection and try again')
  }

  const text = await res.text()
  const json = parseJson(text)

  if (!res.ok) {
    if (json === null && AUTH_CODES.includes(text.trim())) {
      clearSession()
      throw new ApiError(res.status, 'Your session has expired. Please sign in again.', [], 'AUTH')
    }
    if (res.status === 429) {
      throw new ApiError(429, json?.message ?? 'Too many requests. Wait a minute and try again.')
    }
    throw new ApiError(res.status, json?.message ?? `Request failed (${res.status})`, json?.errors ?? [], json?.code, json?.reason)
  }
  return (json?.data ?? json) as T
}

export const api = {
  get: <T>(path: string, auth = true) => request<T>('GET', path, undefined, auth),
  post: <T>(path: string, body?: unknown, auth = true) => request<T>('POST', path, body ?? {}, auth),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {}),
  del: <T>(path: string) => request<T>('DELETE', path),
}
