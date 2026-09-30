import { Link, useNavigate, useSearchParams } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, LayoutDashboard, LogOut, ScrollText, ShieldAlert, ShieldCheck, Building2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/misc'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Logo } from '@/components/common/logo'
import { ThemeToggle } from '@/components/common/theme-toggle'
import { ErrorBox } from '@/components/common/errors'
import { ApiError } from '@/lib/api'
import { useAdminAccess } from '@/lib/queries'
import { clearSession } from '@/lib/session'
import AdminOverview from './AdminOverview'
import AdminCompanies from './AdminCompanies'
import AdminAudit from './AdminAudit'

export default function AdminPage() {
  const access = useAdminAccess()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? 'overview'
  const qc = useQueryClient()
  const navigate = useNavigate()

  const denied = access.error instanceof ApiError && access.error.status === 403

  return (
    <div className="min-h-dvh">
      <header className="sticky top-0 z-30 border-b bg-background/80 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-6xl items-center gap-3 px-4 md:px-8">
          <Logo to="/admin" />
          <Badge tone="brand" className="hidden sm:inline-flex"><ShieldCheck className="size-3" /> Platform admin</Badge>
          <div className="ml-auto flex items-center gap-1">
            <Button variant="ghost" size="sm" asChild><Link to="/app"><ArrowLeft /> Company dashboard</Link></Button>
            <ThemeToggle />
            <Button variant="ghost" size="icon" aria-label="Sign out" onClick={() => { clearSession(); qc.clear(); navigate('/login') }}><LogOut /></Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6 md:px-8 md:py-8">
        {access.isLoading ? (
          <div className="space-y-3"><Skeleton className="h-8 w-56" /><Skeleton className="h-40" /></div>
        ) : denied ? (
          <div className="mx-auto max-w-md pt-16 text-center">
            <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive"><ShieldAlert /></div>
            <h1 className="mt-4 text-xl font-semibold">Admins only</h1>
            <p className="mt-1 text-sm text-muted-foreground">This area needs the platform SUPERADMIN role.</p>
            <Button asChild className="mt-6"><Link to="/app">Back to dashboard</Link></Button>
          </div>
        ) : access.error ? (
          <ErrorBox error={access.error} />
        ) : (
          <>
            <div className="mb-6">
              <h1 className="text-2xl font-semibold tracking-tight">Platform</h1>
              <p className="text-sm text-muted-foreground">Every company on AirdropX. Actions here are recorded in the audit log.</p>
            </div>
            <Tabs value={tab} onValueChange={(v) => setParams({ tab: v }, { replace: true })}>
              <TabsList>
                <TabsTrigger value="overview"><LayoutDashboard className="size-4" /> Overview</TabsTrigger>
                <TabsTrigger value="companies"><Building2 className="size-4" /> Companies</TabsTrigger>
                <TabsTrigger value="audit"><ScrollText className="size-4" /> Audit log</TabsTrigger>
              </TabsList>
              <TabsContent value="overview"><AdminOverview onGo={(t) => setParams({ tab: t })} /></TabsContent>
              <TabsContent value="companies"><AdminCompanies /></TabsContent>
              <TabsContent value="audit"><AdminAudit /></TabsContent>
            </Tabs>
          </>
        )}
      </main>
    </div>
  )
}
