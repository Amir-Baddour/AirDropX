import { CheckCircle2, CircleDot, Inbox, ListChecks, Lock, Rocket, Unlock, Users, XCircle, History } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { EmptyState, Skeleton } from '@/components/ui/misc'
import { ErrorBox } from '@/components/common/errors'
import { useEvents } from '@/lib/queries'
import type { Airdrop } from '@/lib/types'
import { formatDate, timeAgo } from '@/lib/utils'

function iconFor(type: string) {
  if (type.startsWith('CLAIM_REJECTED') || type === 'CANCELLED' || type.includes('FAIL')) return <XCircle className="text-destructive" />
  if (type === 'CLAIMS_OPENED') return <Unlock className="text-success" />
  if (type === 'CLAIMS_CLOSED') return <Lock />
  if (type.startsWith('CLAIM_')) return <Inbox className="text-primary" />
  if (type.startsWith('TASK_')) return <ListChecks className="text-primary" />
  if (type === 'RECIPIENTS_ADDED') return <Users className="text-info" />
  if (type === 'LAUNCHED') return <Rocket className="text-warning" />
  if (type === 'COMPLETED' || type === 'VALIDATED') return <CheckCircle2 className="text-success" />
  return <CircleDot className="text-muted-foreground" />
}

export default function TimelineTab({ airdrop }: { airdrop: Airdrop }) {
  const { data, isLoading, error } = useEvents(airdrop.id, airdrop.status === 'PROCESSING')
  if (isLoading) return <Skeleton className="h-48" />
  if (error) return <ErrorBox error={error} />
  if (!data?.length) return <EmptyState icon={<History />} title="No events yet" />
  return (
    <Card className="p-5">
      <ol className="relative space-y-5 before:absolute before:bottom-2 before:left-[15px] before:top-2 before:w-px before:bg-border">
        {data.map((e) => (
          <li key={e.id} className="relative flex gap-4">
            <span className="z-10 flex size-8 shrink-0 items-center justify-center rounded-full border bg-card [&_svg]:size-4">{iconFor(e.type)}</span>
            <div className="min-w-0 pt-1">
              <p className="text-sm"><span className="font-mono text-xs text-muted-foreground">{e.type}</span></p>
              <p className="text-sm">{e.message}</p>
              <p className="text-xs text-muted-foreground" title={formatDate(e.created_at)}>{timeAgo(e.created_at)}</p>
            </div>
          </li>
        ))}
      </ol>
    </Card>
  )
}
