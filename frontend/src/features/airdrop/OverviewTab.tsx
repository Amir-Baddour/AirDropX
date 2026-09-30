import { Link } from 'react-router'
import { AlertTriangle, CheckCircle2, CircleDashed, Clock, ExternalLink, XCircle } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { NumberTicker } from '@/components/common/number-ticker'
import { useClaims } from '@/lib/queries'
import type { Airdrop, RecipientStatus } from '@/lib/types'
import { formatAmount } from '@/lib/utils'

// Status colors are reserved for state and always shown with an icon + label.
const SEGMENTS: { status: RecipientStatus; label: string; color: string; icon: typeof CheckCircle2 }[] = [
  { status: 'COMPLETED', label: 'Paid', color: 'var(--success)', icon: CheckCircle2 },
  { status: 'PROCESSING', label: 'Sending', color: 'var(--warning)', icon: Clock },
  { status: 'PENDING', label: 'Pending', color: 'var(--muted-foreground)', icon: CircleDashed },
  { status: 'FAILED', label: 'Failed', color: 'var(--destructive)', icon: AlertTriangle },
  { status: 'CANCELLED', label: 'Cancelled', color: 'color-mix(in oklab, var(--destructive) 45%, var(--muted))', icon: XCircle },
]

export default function OverviewTab({ airdrop, onGo }: { airdrop: Airdrop; onGo: (tab: string) => void }) {
  const stats = airdrop.stats
  const total = stats?.total_recipients ?? 0
  const by = stats?.by_status ?? {}
  const paid = by.COMPLETED ?? 0
  const pct = total ? Math.round((paid / total) * 100) : 0
  const claims = useClaims(airdrop.id)
  const c = claims.data?.counts

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="grid grid-cols-2 gap-3 lg:col-span-3 lg:grid-cols-4">
        <Stat label="Recipients" value={total} />
        <Stat label={`Total ${airdrop.token_symbol}`} text={formatAmount(stats?.total_amount ?? '0', 2)} />
        <Stat label="Paid" value={paid} suffix={total ? ` / ${total}` : ''} />
        <Stat label="Claims to review" value={c?.NEEDS_REVIEW ?? 0} highlight={(c?.NEEDS_REVIEW ?? 0) > 0} />
      </div>

      <Card className="lg:col-span-2">
        <CardHeader>
          <CardTitle>Payout progress</CardTitle>
          <CardDescription>{total ? `${pct}% of recipients paid` : 'No recipients yet'}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          {total > 0 && (
            <>
              {/* one segmented bar, 2px surface gaps between segments */}
              <div className="flex h-3 w-full gap-[2px] overflow-hidden rounded-full" role="img" aria-label="Recipients by status">
                {SEGMENTS.filter((s) => (by[s.status] ?? 0) > 0).map((s) => (
                  <div
                    key={s.status}
                    className="h-full first:rounded-l-full last:rounded-r-full transition-[flex-grow] duration-700"
                    style={{ flexGrow: by[s.status] ?? 0, background: s.color }}
                    title={`${s.label}: ${by[s.status]}`}
                  />
                ))}
              </div>
              <ul className="grid grid-cols-2 gap-x-6 gap-y-2 sm:grid-cols-3">
                {SEGMENTS.map(({ status, label, color, icon: Icon }) => (
                  <li key={status} className="flex items-center gap-2 text-sm">
                    <Icon className="size-4" style={{ color }} />
                    <span className="text-muted-foreground">{label}</span>
                    <span className="ml-auto font-medium tabular-nums">{by[status] ?? 0}</span>
                  </li>
                ))}
              </ul>
            </>
          )}
          {total === 0 && airdrop.status === 'DRAFT' && (
            <div className="flex flex-wrap gap-2">
              <Button size="sm" onClick={() => onGo('recipients')}>Add recipients</Button>
              <Button size="sm" variant="outline" onClick={() => onGo('tasks')}>Or add tasks and open claims</Button>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Public claims</CardTitle>
          <CardDescription>
            {airdrop.claims_open
              ? `Open · ${formatAmount(airdrop.claim_amount)} ${airdrop.token_symbol} per claim`
              : 'Closed'}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <dl className="grid grid-cols-3 gap-2 text-center">
            {(['APPROVED', 'NEEDS_REVIEW', 'REJECTED'] as const).map((k) => (
              <div key={k} className="rounded-md bg-muted/50 py-2">
                <dd className="text-lg font-semibold tabular-nums">{c?.[k] ?? 0}</dd>
                <dt className="text-[11px] text-muted-foreground">{k === 'NEEDS_REVIEW' ? 'Review' : k.charAt(0) + k.slice(1).toLowerCase()}</dt>
              </div>
            ))}
          </dl>
          <div className="flex flex-wrap gap-2">
            <Button size="sm" variant="outline" onClick={() => onGo('claims')}>Manage claims</Button>
            {airdrop.claims_open && (
              <Button size="sm" variant="ghost" asChild>
                <Link to={`/claim/${airdrop.id}`} target="_blank"><ExternalLink /> Public page</Link>
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function Stat({ label, value, text, suffix, highlight }: { label: string; value?: number; text?: string; suffix?: string; highlight?: boolean }) {
  return (
    <Card className={highlight ? 'border-warning/40 p-4' : 'p-4'}>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">
        {text ?? <NumberTicker value={value ?? 0} />}
        {suffix && <span className="text-sm font-normal text-muted-foreground">{suffix}</span>}
      </p>
    </Card>
  )
}
