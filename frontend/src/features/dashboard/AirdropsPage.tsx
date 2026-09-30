import { useMemo, useState } from 'react'
import { Link } from 'react-router'
import { ArrowRight, Gift, Plus, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { EmptyState, Skeleton } from '@/components/ui/misc'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { PageHeader } from '@/components/layout/PageHeader'
import { AirdropStatusBadge } from '@/components/common/status'
import { ErrorBox } from '@/components/common/errors'
import { NumberTicker } from '@/components/common/number-ticker'
import { useAirdrops } from '@/lib/queries'
import type { AirdropStatus } from '@/lib/types'
import { cn, timeAgo } from '@/lib/utils'
import CreateAirdropDialog from './CreateAirdropDialog'

const FILTERS: (AirdropStatus | 'ALL')[] = ['ALL', 'DRAFT', 'VALIDATED', 'PROCESSING', 'COMPLETED', 'CANCELLED']

export default function AirdropsPage() {
  const { data, isLoading, error } = useAirdrops()
  const [filter, setFilter] = useState<AirdropStatus | 'ALL'>('ALL')
  const [q, setQ] = useState('')

  const counts = useMemo(() => {
    const c: Record<string, number> = { ALL: data?.length ?? 0 }
    data?.forEach((a) => (c[a.status] = (c[a.status] ?? 0) + 1))
    return c
  }, [data])

  const rows = useMemo(
    () =>
      (data ?? []).filter(
        (a) => (filter === 'ALL' || a.status === filter) && (!q || `${a.name} ${a.token_symbol}`.toLowerCase().includes(q.toLowerCase())),
      ),
    [data, filter, q],
  )

  const create = <CreateAirdropDialog trigger={<Button><Plus /> New airdrop</Button>} />

  return (
    <>
      <PageHeader title="Airdrops" description="Create campaigns, verify claimants and send payouts." actions={data?.length ? create : undefined} />

      {data && data.length > 0 && (
        <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
          {[
            { label: 'Total airdrops', value: counts.ALL },
            { label: 'Accepting claims', value: data.filter((a) => a.claims_open).length },
            { label: 'Processing', value: counts.PROCESSING ?? 0 },
            { label: 'Completed', value: counts.COMPLETED ?? 0 },
          ].map((s) => (
            <Card key={s.label} className="p-4">
              <p className="text-xs text-muted-foreground">{s.label}</p>
              <NumberTicker value={s.value} className="mt-1 block text-2xl font-semibold tabular-nums" />
            </Card>
          ))}
        </div>
      )}

      {isLoading ? (
        <div className="space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}</div>
      ) : error ? (
        <ErrorBox error={error} />
      ) : !data?.length ? (
        <EmptyState
          icon={<Gift />}
          title="No airdrops yet"
          description="Create your first airdrop, add tasks, open claims and let real users earn your tokens."
          action={create}
        />
      ) : (
        <Card>
          <div className="flex flex-col gap-3 border-b p-3 sm:flex-row sm:items-center">
            <div className="flex gap-1 overflow-x-auto">
              {FILTERS.map((f) => (
                <button
                  key={f}
                  onClick={() => setFilter(f)}
                  className={cn(
                    'rounded-md px-2.5 py-1 text-xs font-medium whitespace-nowrap transition-colors cursor-pointer',
                    filter === f ? 'bg-accent text-foreground' : 'text-muted-foreground hover:text-foreground',
                  )}
                >
                  {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
                  <span className="ml-1.5 text-muted-foreground">{counts[f] ?? 0}</span>
                </button>
              ))}
            </div>
            <div className="relative sm:ml-auto sm:w-60">
              <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search" className="h-8 pl-8" />
            </div>
          </div>
          <Table>
            <THead>
              <TR className="hover:bg-transparent"><TH>Name</TH><TH>Status</TH><TH className="hidden sm:table-cell">Claims</TH><TH className="hidden md:table-cell">Updated</TH><TH /></TR>
            </THead>
            <TBody>
              {rows.map((a) => (
                <TR key={a.id} className="group">
                  <TD>
                    <Link to={`/app/airdrops/${a.id}`} className="font-medium after:absolute after:inset-0 relative">
                      {a.name}
                    </Link>
                    <p className="font-mono text-xs text-muted-foreground">{a.token_symbol}</p>
                  </TD>
                  <TD><AirdropStatusBadge status={a.status} /></TD>
                  <TD className="hidden sm:table-cell">
                    {a.claims_open ? <span className="text-success text-xs font-medium">● Open</span> : <span className="text-xs text-muted-foreground">Closed</span>}
                  </TD>
                  <TD className="hidden text-muted-foreground md:table-cell">{timeAgo(a.updated_at)}</TD>
                  <TD className="w-10 text-right"><ArrowRight className="inline size-4 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" /></TD>
                </TR>
              ))}
              {rows.length === 0 && (
                <TR><TD colSpan={5} className="py-10 text-center text-muted-foreground">No airdrops match this filter.</TD></TR>
              )}
            </TBody>
          </Table>
        </Card>
      )}
    </>
  )
}
