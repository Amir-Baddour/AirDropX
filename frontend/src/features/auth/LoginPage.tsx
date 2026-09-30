import { useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { KeyRound } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/input'
import { GlowBackground } from '@/components/common/glow-background'
import { Logo } from '@/components/common/logo'
import { GOOGLE_CLIENT_ID, startGoogleLogin, useSession } from '@/lib/auth'
import { saveSession } from '@/lib/session'

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-4" aria-hidden>
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.27-4.74 3.27-8.1z" />
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23z" />
      <path fill="#FBBC05" d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.18a11 11 0 0 0 0 9.88l3.66-2.84z" />
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1A11 11 0 0 0 2.18 7.06l3.66 2.84C6.71 7.31 9.14 5.38 12 5.38z" />
    </svg>
  )
}

function decodeSub(jwt: string): string | null {
  try {
    const payload = JSON.parse(atob(jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.sub === 'string' ? payload.sub : null
  } catch {
    return null
  }
}

export default function LoginPage() {
  const { isAuthenticated } = useSession()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [showToken, setShowToken] = useState(false)
  const [token, setToken] = useState('')
  const returnTo = params.get('next') ?? '/app'

  if (isAuthenticated) return <Navigate to={returnTo} replace />

  return (
    <div className="relative flex min-h-dvh items-center justify-center px-4">
      <GlowBackground />
      <div className="glass w-full max-w-sm rounded-2xl p-8">
        <Logo className="mb-8 text-lg" />
        <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage your company's airdrops, tasks and claims.</p>

        <Button
          className="mt-8 w-full bg-white text-zinc-900 hover:bg-zinc-100"
          size="lg"
          disabled={!GOOGLE_CLIENT_ID}
          onClick={() => startGoogleLogin(returnTo)}
        >
          <GoogleIcon /> Continue with Google
        </Button>
        {!GOOGLE_CLIENT_ID && (
          <p className="mt-2 text-center text-xs text-muted-foreground">Google sign-in isn't configured on this server yet.</p>
        )}

        <div className="mt-6 border-t pt-4">
          <button
            className="flex w-full items-center justify-center gap-2 text-xs text-muted-foreground hover:text-foreground cursor-pointer"
            onClick={() => setShowToken((s) => !s)}
          >
            <KeyRound className="size-3.5" /> Developer: sign in with an access token
          </button>
          {showToken && (
            <form
              className="mt-3 grid gap-2"
              onSubmit={(e) => {
                e.preventDefault()
                const t = token.trim()
                const sub = decodeSub(t)
                if (!sub) return toast.error('That is not a valid JWT')
                saveSession(t, { id: sub, username: 'developer', pfp: null })
                navigate(returnTo, { replace: true })
              }}
            >
              <Textarea value={token} onChange={(e) => setToken(e.target.value)} placeholder="eyJhbGciOi…" className="font-mono text-xs" rows={3} />
              <Button type="submit" variant="secondary" size="sm">Use token</Button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
