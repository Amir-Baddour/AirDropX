import { Ban, RotateCcw, ScrollText, StickyNote } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { EmptyState, Skeleton } from '@/components/ui/misc'
import { ErrorBox } from '@/components/common/errors'
import { useAdminAudit } from '@/lib/queries'
import { formatDate, timeAgo } from '@/lib/utils'

const LABELS: Record<string, { label: string; icon: typeof Ban; tone: string }> = {
  COMPANY_SUSPENDED: { label: 'Suspended company', icon: Ban, tone: 'text-destructive' },
  COMPANY_RESTORED: { label: 'Restored company', icon: RotateCcw, tone: 'text-success' },
}

export default function AdminAudit() {
  const { data, isLoading, error } = useAdminAudit()
  if (isLoading) return <Skeleton className="h-48" />
  if (error) return <ErrorBox error={error} />
  if (!data?.length) return <EmptyState icon={<ScrollText />} title="No admin actions yet" description="Every suspend and restore is recorded here with who did it and why." />
  return (
    <Card className="divide-y">
      {data.map((a) => {
        const meta = LABELS[a.action] ?? { label: a.action, icon: StickyNote, tone: 'text-muted-foreground' }
        return (
          <div key={a.id} className="flex gap-3 p-4">
            <meta.icon className={`mt-0.5 size-4 shrink-0 ${meta.tone}`} />
            <div className="min-w-0 flex-1">
              <p className="text-sm"><span className="font-medium">{a.admin_username ?? 'Admin'}</span> <span className="text-muted-foreground">{meta.label.toLowerCase()}</span></p>
              {a.detail && <p className="text-sm text-muted-foreground">{a.detail}</p>}
              <p className="mt-0.5 font-mono text-[11px] text-muted-foreground">{a.target_type} {a.target_id}</p>
            </div>
            <span className="shrink-0 text-xs text-muted-foreground" title={formatDate(a.created_at)}>{timeAgo(a.created_at)}</span>
          </div>
        )
      })}
    </Card>
  )
}
