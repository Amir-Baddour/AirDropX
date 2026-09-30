import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { GlowBackground } from '@/components/common/glow-background'
import { api, ApiError } from '@/lib/api'
import { saveSession } from '@/lib/session'
import type { SessionUser } from '@/lib/types'

interface LoginResponse extends SessionUser {
  token?: { access_token: string; expires_at: string | number }
}

export default function AuthCallbackPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  // Check what Google sent back once, before any network call.
  const [initial] = useState(() => {
    const code = params.get('code')
    const state = params.get('state')
    const expected = sessionStorage.getItem('oauth.state')
    if (params.get('error')) return { error: 'Google sign-in was cancelled.' }
    if (!code) return { error: 'Missing sign-in code from Google.' }
    if (!state || state !== expected) return { error: 'Sign-in link expired or was tampered with. Please try again.' }
    return { code }
  })
  const [fetchError, setFetchError] = useState<string | null>(null)
  const error = initial.error ?? fetchError
  const started = useRef(false)

  useEffect(() => {
    // React StrictMode runs effects twice; a Google code can only be used once.
    if (started.current || !initial.code) return
    started.current = true
    const returnTo = sessionStorage.getItem('oauth.returnTo') ?? '/app'
    sessionStorage.removeItem('oauth.state')
    api
      .post<LoginResponse>('/auth/login/oauth', { provider: 'GOOGLE', code: encodeURIComponent(initial.code) }, false)
      .then((user) => {
        if (!user.token?.access_token) throw new ApiError(500, 'The server did not return a session')
        saveSession(user.token.access_token, { id: user.id, username: user.username, pfp: user.pfp, role: user.role }, user.token.expires_at)
        navigate(returnTo, { replace: true })
      })
      .catch((e) => setFetchError(e instanceof ApiError ? e.message : 'Sign-in failed'))
  }, [initial.code, navigate])

  return (
    <div className="relative flex min-h-dvh items-center justify-center px-4">
      <GlowBackground />
      <div className="glass w-full max-w-sm rounded-2xl p-8 text-center">
        {error ? (
          <>
            <p className="font-medium">Couldn't sign you in</p>
            <p className="mt-1 text-sm text-muted-foreground">{error}</p>
            <Button asChild className="mt-6"><Link to="/login">Try again</Link></Button>
          </>
        ) : (
          <div className="flex flex-col items-center gap-3 text-sm text-muted-foreground">
            <Loader2 className="size-6 animate-spin text-primary" /> Signing you in…
          </div>
        )}
      </div>
    </div>
  )
}
