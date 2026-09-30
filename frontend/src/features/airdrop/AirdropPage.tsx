import { Link, useParams, useSearchParams } from 'react-router'
import { ArrowLeft, Ban, CheckCircle2, ListChecks, Rocket, RotateCcw, Users, History, LayoutDashboard, Inbox } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Confirm } from '@/components/ui/confirm'
import { Skeleton } from '@/components/ui/misc'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { AirdropStatusBadge } from '@/components/common/status'
import { ErrorBox, toastError } from '@/components/common/errors'
import { useAirdrop, useAirdropAction, type LifecycleAction } from '@/lib/queries'
import type { Airdrop } from '@/lib/types'
import LifecycleStepper from './LifecycleStepper'
import { Linkify } from '@/components/common/linkify'
import OverviewTab from './OverviewTab'
import RecipientsTab from './RecipientsTab'
import TasksTab from './TasksTab'
import ClaimsTab from './ClaimsTab'
import TimelineTab from './TimelineTab'

const TABS = [
  { value: 'overview', label: 'Overview', icon: LayoutDashboard },
  { value: 'recipients', label: 'Recipients', icon: Users },
  { value: 'tasks', label: 'Tasks', icon: ListChecks },
  { value: 'claims', label: 'Claims', icon: Inbox },
  { value: 'timeline', label: 'Timeline', icon: History },
]

export default function AirdropPage() {
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? 'overview'
  const { data: airdrop, isLoading, error } = useAirdrop(id)

  if (isLoading) return <div className="space-y-4"><Skeleton className="h-8 w-64" /><Skeleton className="h-20 w-full" /><Skeleton className="h-64 w-full" /></div>
  if (error || !airdrop) return <ErrorBox error={error} />

  return (
    <>
      <Link to="/app" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Airdrops
      </Link>
      <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="truncate text-2xl font-semibold tracking-tight">{airdrop.name}</h1>
            <AirdropStatusBadge status={airdrop.status} />
            {airdrop.claims_open && <span className="text-xs font-medium text-success">● Accepting claims</span>}
          </div>
          <p className="mt-1 break-words text-sm text-muted-foreground">
            <span className="font-mono">{airdrop.token_symbol}</span>
            {airdrop.description && <> · <Linkify text={airdrop.description} /></>}
          </p>
        </div>
        <LifecycleActions airdrop={airdrop} />
      </div>

      <LifecycleStepper airdrop={airdrop} />

      <Tabs value={tab} onValueChange={(v) => setParams({ tab: v }, { replace: true })} className="mt-8">
        <TabsList>
          {TABS.map(({ value, label, icon: Icon }) => (
            <TabsTrigger key={value} value={value}><Icon className="size-4" />{label}</TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="overview"><OverviewTab airdrop={airdrop} onGo={(t) => setParams({ tab: t })} /></TabsContent>
        <TabsContent value="recipients"><RecipientsTab airdrop={airdrop} /></TabsContent>
        <TabsContent value="tasks"><TasksTab airdrop={airdrop} /></TabsContent>
        <TabsContent value="claims"><ClaimsTab airdrop={airdrop} /></TabsContent>
        <TabsContent value="timeline"><TimelineTab airdrop={airdrop} /></TabsContent>
      </Tabs>
    </>
  )
}

function LifecycleActions({ airdrop }: { airdrop: Airdrop }) {
  const action = useAirdropAction(airdrop.id)
  const run = (a: LifecycleAction, msg: string) => action.mutateAsync(a).then(() => toast.success(msg)).catch((e) => { toastError(e); throw e })
  const s = airdrop.status
  const recipients = airdrop.stats?.total_recipients ?? 0

  return (
    <div className="flex flex-wrap gap-2">
      {s === 'DRAFT' && (
        <Confirm
          trigger={<Button disabled={recipients === 0} title={recipients === 0 ? 'Add recipients first' : undefined}><CheckCircle2 /> Validate</Button>}
          title="Validate this airdrop?"
          description={<>This freezes the recipient list ({recipients} recipient{recipients === 1 ? '' : 's'}){airdrop.claims_open ? ' and closes public claims' : ''}. You can move it back to draft before launch.</>}
          confirmLabel="Validate"
          onConfirm={() => run('validate', 'Airdrop validated')}
        />
      )}
      {s === 'VALIDATED' && (
        <>
          <Confirm
            trigger={<Button variant="outline"><RotateCcw /> Back to draft</Button>}
            title="Move back to draft?"
            description="You'll be able to edit recipients, tasks and claims again."
            confirmLabel="Move to draft"
            onConfirm={() => run('reopen', 'Moved back to draft')}
          />
          <Confirm
            trigger={<Button variant="brand"><Rocket /> Launch</Button>}
            title="Launch payouts?"
            description={<>The worker will start sending {airdrop.stats?.total_amount ?? ''} {airdrop.token_symbol} to {recipients} recipient(s). This can't be undone (payouts are mocked).</>}
            confirmLabel="Launch"
            onConfirm={() => run('launch', 'Launched: payouts started')}
          />
        </>
      )}
      {(s === 'DRAFT' || s === 'VALIDATED' || s === 'PROCESSING') && (
        <Confirm
          trigger={<Button variant="ghost" className="text-destructive hover:text-destructive"><Ban /> Cancel</Button>}
          title="Cancel this airdrop?"
          description="Unpaid recipients are cancelled and claims close. This is final."
          confirmLabel="Cancel airdrop"
          destructive
          onConfirm={() => run('cancel', 'Airdrop cancelled')}
        />
      )}
    </div>
  )
}
