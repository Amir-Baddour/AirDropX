import { NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { BookOpen, Building2, LayoutGrid, LogOut, Menu, ShieldCheck, UserRound } from 'lucide-react'
import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/misc'
import { Logo } from '@/components/common/logo'
import { ThemeToggle } from '@/components/common/theme-toggle'
import { ErrorBox } from '@/components/common/errors'
import CompanyOnboarding from '@/features/dashboard/CompanyOnboarding'
import CompanySuspended from '@/features/dashboard/CompanySuspended'
import { useSession } from '@/lib/auth'
import { ApiError } from '@/lib/api'
import { useAdminAccess, useCompany } from '@/lib/queries'
import { clearSession } from '@/lib/session'
import { cn } from '@/lib/utils'

const nav = [
  { to: '/app', label: 'Airdrops', icon: LayoutGrid, end: true },
  { to: '/app/company', label: 'Company', icon: Building2 },
  { to: '/app/profile', label: 'Profile', icon: UserRound },
  { to: '/docs', label: 'How it works', icon: BookOpen },
]

export default function AppLayout() {
  const company = useCompany()
  const isAdmin = useAdminAccess().isSuccess
  const { user } = useSession()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)

  const signOut = () => {
    clearSession()
    qc.clear()
    navigate('/login')
  }

  const noCompany = company.error instanceof ApiError && company.error.status === 404
  const suspended = company.error instanceof ApiError && company.error.code === 'COMPANY_SUSPENDED'
  // Your own profile stays reachable before you create a company, or while the company is suspended.
  const onProfile = pathname === '/app/profile'
  const label = user?.displayName || user?.username
  const links = isAdmin ? [...nav, { to: '/admin', label: 'Platform admin', icon: ShieldCheck, end: false }] : nav

  const sidebar = (
    <aside className="flex h-full w-60 flex-col border-r bg-card/50 px-3 py-4">
      <Logo className="px-2" to="/app" />
      <div className="mt-6 rounded-md border bg-background/60 px-3 py-2">
        <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Company</p>
        {company.isLoading ? <Skeleton className="mt-1 h-4 w-28" /> : <p className="truncate text-sm font-medium">{company.data?.name ?? '—'}</p>}
      </div>
      <nav className="mt-4 grid gap-0.5">
        {links.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={() => setOpen(false)}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-foreground',
                isActive && 'bg-accent text-foreground font-medium',
              )
            }
          >
            <Icon className="size-4" /> {label}
          </NavLink>
        ))}
      </nav>
      <div className="mt-auto flex items-center gap-2 border-t pt-3">
        <div className="flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-full bg-muted text-xs font-semibold uppercase">
          {user?.pfp ? <img src={user.pfp} alt="" className="size-full object-cover" referrerPolicy="no-referrer" /> : label?.[0] ?? '?'}
        </div>
        <p className="min-w-0 flex-1 truncate text-sm">{label}</p>
        <ThemeToggle />
        <Button variant="ghost" size="icon" onClick={signOut} aria-label="Sign out"><LogOut /></Button>
      </div>
    </aside>
  )

  return (
    <div className="flex min-h-dvh">
      <div className="sticky top-0 hidden h-dvh md:block">{sidebar}</div>
      {open && (
        <div className="fixed inset-0 z-40 md:hidden" onClick={() => setOpen(false)}>
          <div className="absolute inset-0 bg-black/50" />
          <div className="relative h-full w-60" onClick={(e) => e.stopPropagation()}>{sidebar}</div>
        </div>
      )}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center gap-3 border-b px-4 md:hidden">
          <Button variant="ghost" size="icon" onClick={() => setOpen(true)} aria-label="Menu"><Menu /></Button>
          <Logo to="/app" />
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 md:px-8 md:py-8">
          {onProfile ? (
            <Outlet />
          ) : company.isLoading ? (
            <div className="space-y-3"><Skeleton className="h-8 w-48" /><Skeleton className="h-40 w-full" /></div>
          ) : noCompany ? (
            <CompanyOnboarding isAdmin={isAdmin} />
          ) : suspended ? (
            <CompanySuspended reason={(company.error as ApiError).reason ?? null} />
          ) : company.error ? (
            <ErrorBox error={company.error} />
          ) : (
            <Outlet />
          )}
        </main>
      </div>
    </div>
  )
}
