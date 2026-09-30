import { AlertTriangle, CheckCircle2, CircleDashed, Clock, XCircle, FileCheck2 } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/misc'
import { NumberTicker } from '@/components/common/number-ticker'
import { ErrorBox } from '@/components/common/errors'
import { useAdminActivity, useAdminStats } from '@/lib/queries'
import { timeAgo } from '@/lib/utils'

type Segment = { key: string; label: string; value: number; color: string; icon: typeof CheckCircle2 }

/** One segmented bar + labelled legend (status colors always come with an icon and a label). */
function Breakdown({ title, description, segments }: { title: string; description: string; segments: Segment[] }) {
  const total = segments.reduce((s, x) => s + x.value, 0)
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex h-3 w-full gap-[2px] overflow-hidden rounded-full bg-muted" role="img" aria-label={title}>
          {total > 0 && segments.filter((s) => s.value > 0).map((s) => (
            <div key={s.key} className="h-full first:rounded-l-full last:rounded-r-full" style={{ flexGrow: s.value, background: s.color }} title={`${s.label}: ${s.value}`} />
          ))}
        </div>
        <ul className="grid gap-2">
          {segments.map(({ key, label, value, color, icon: Icon }) => (
            <li key={key} className="flex items-center gap-2 text-sm">
              <Icon className="size-4" style={{ color }} />
              <span className="text-muted-foreground">{label}</span>
              <span className="ml-auto font-medium tabular-nums">{value}</span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

export default function AdminOverview({ onGo }: { onGo: (tab: string) => void }) {
  const stats = useAdminStats()
  const activity = useAdminActivity()
  if (stats.isLoading) return <div className="grid gap-3 sm:grid-cols-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-24" />)}</div>
  if (stats.error || !stats.data) return <ErrorBox error={stats.error} />
  const s = stats.data
  const ok = 'var(--success)', warn = 'var(--warning)', bad = 'var(--destructive)', neutral = 'var(--muted-foreground)', info = 'var(--info)'

  const tiles = [
    { label: 'Active companies', value: s.companies.ACTIVE },
    { label: 'Suspended', value: s.companies.SUSPENDED, alert: s.companies.SUSPENDED > 0, go: 'companies' },
    { label: 'Users', value: s.users },
    { label: 'Claims (24h)', value: s.claims_last_24h },
  ]

  return (
    <div className="grid gap-4">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {tiles.map((t) => (
          <Card
            key={t.label}
            className={`p-4 ${t.alert ? 'border-destructive/40' : ''} ${t.go ? 'cursor-pointer hover:bg-accent/40' : ''}`}
            onClick={t.go ? () => onGo(t.go!) : undefined}
          >
            <p className="text-xs text-muted-foreground">{t.label}</p>
            <NumberTicker value={t.value} className="mt-1 block text-2xl font-semibold tabular-nums" />
          </Card>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Breakdown
          title="Airdrops"
          description="By lifecycle status"
          segments={[
            { key: 'D', label: 'Draft', value: s.airdrops.DRAFT, color: neutral, icon: CircleDashed },
            { key: 'V', label: 'Validated', value: s.airdrops.VALIDATED, color: info, icon: CheckCircle2 },
            { key: 'P', label: 'Processing', value: s.airdrops.PROCESSING, color: warn, icon: Clock },
            { key: 'C', label: 'Completed', value: s.airdrops.COMPLETED, color: ok, icon: CheckCircle2 },
            { key: 'X', label: 'Cancelled', value: s.airdrops.CANCELLED, color: bad, icon: XCircle },
          ]}
        />
        <Breakdown
          title="Claims"
          description="All public claims"
          segments={[
            { key: 'A', label: 'Approved', value: s.claims.APPROVED, color: ok, icon: CheckCircle2 },
            { key: 'N', label: 'Needs review', value: s.claims.NEEDS_REVIEW, color: warn, icon: FileCheck2 },
            { key: 'R', label: 'Rejected', value: s.claims.REJECTED, color: bad, icon: XCircle },
          ]}
        />
        <Breakdown
          title="Payouts"
          description="Recipients across all airdrops"
          segments={[
            { key: 'C', label: 'Paid', value: s.recipients.COMPLETED, color: ok, icon: CheckCircle2 },
            { key: 'P', label: 'Sending', value: s.recipients.PROCESSING, color: warn, icon: Clock },
            { key: 'W', label: 'Pending', value: s.recipients.PENDING, color: neutral, icon: CircleDashed },
            { key: 'F', label: 'Failed', value: s.recipients.FAILED, color: bad, icon: AlertTriangle },
          ]}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Live activity</CardTitle>
          <CardDescription>Latest events from every company</CardDescription>
        </CardHeader>
        <CardContent>
          {activity.isLoading ? <Skeleton className="h-32" /> : !activity.data?.length ? (
            <p className="py-6 text-center text-sm text-muted-foreground">No activity yet.</p>
          ) : (
            <ul className="divide-y">
              {activity.data.map((e) => (
                <li key={e.id} className="flex flex-wrap items-baseline gap-x-3 gap-y-0.5 py-2.5 text-sm">
                  <span className="font-medium">{e.company_name}</span>
                  <span className="text-muted-foreground">·</span>
                  <span className="text-muted-foreground">{e.airdrop_name}</span>
                  <span className="font-mono text-[11px] text-muted-foreground">{e.type}</span>
                  <span className="basis-full text-muted-foreground sm:basis-auto sm:flex-1">{e.message}</span>
                  <span className="text-xs text-muted-foreground">{timeAgo(e.created_at)}</span>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
