import { useEffect, useState } from 'react'
import { Ban, RotateCcw, Search } from 'lucide-react'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Confirm } from '@/components/ui/confirm'
import { Input, NativeSelect, Textarea } from '@/components/ui/input'
import { EmptyState, Skeleton } from '@/components/ui/misc'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { ErrorBox, toastError } from '@/components/common/errors'
import { useAdminCompanies, useCompanyModeration } from '@/lib/queries'
import type { AdminCompany } from '@/lib/types'
import { formatDate, timeAgo } from '@/lib/utils'

export default function AdminCompanies() {
  const [q, setQ] = useState('')
  const [debounced, setDebounced] = useState('')
  const [status, setStatus] = useState('')
  useEffect(() => {
    const t = setTimeout(() => setDebounced(q.trim()), 300)
    return () => clearTimeout(t)
  }, [q])
  const list = useAdminCompanies(debounced, status)

  return (
    <Card>
      <div className="flex flex-col gap-3 border-b p-3 sm:flex-row sm:items-center">
        <div className="relative sm:w-72">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search company or owner" className="h-8 pl-8" maxLength={100} />
        </div>
        <NativeSelect value={status} onChange={(e) => setStatus(e.target.value)} className="h-8 sm:w-40">
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
        </NativeSelect>
        <span className="text-xs text-muted-foreground sm:ml-auto">{list.data ? `${list.data.length} compan${list.data.length === 1 ? 'y' : 'ies'}` : ''}</span>
      </div>
      {list.isLoading ? <div className="space-y-2 p-4">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-10" />)}</div>
        : list.error ? <ErrorBox error={list.error} className="m-4" />
        : !list.data?.length ? <EmptyState title="No companies found" className="m-4" />
        : (
          <Table>
            <THead>
              <TR className="hover:bg-transparent">
                <TH>Company</TH><TH>Status</TH><TH className="hidden md:table-cell text-right">Airdrops</TH>
                <TH className="hidden md:table-cell text-right">Claims</TH><TH className="hidden lg:table-cell">Created</TH><TH className="text-right">Action</TH>
              </TR>
            </THead>
            <TBody>
              {list.data.map((c) => (
                <TR key={c.id}>
                  <TD>
                    <p className="font-medium">{c.name}</p>
                    <p className="text-xs text-muted-foreground">Owner: {c.owner_username ?? '—'} · {c.members} member{c.members === 1 ? '' : 's'}</p>
                  </TD>
                  <TD>
                    {c.status === 'ACTIVE' ? <Badge tone="success" dot>Active</Badge> : (
                      <div className="space-y-1">
                        <Badge tone="danger" dot>Suspended</Badge>
                        {c.suspended_reason && <p className="max-w-56 text-xs text-muted-foreground" title={c.suspended_reason}>“{c.suspended_reason}” · {timeAgo(c.suspended_at)}</p>}
                      </div>
                    )}
                  </TD>
                  <TD className="hidden md:table-cell text-right tabular-nums">{c.airdrops}</TD>
                  <TD className="hidden md:table-cell text-right tabular-nums">{c.claims}</TD>
                  <TD className="hidden lg:table-cell text-muted-foreground">{formatDate(c.created_at)}</TD>
                  <TD className="text-right"><ModerationButton company={c} /></TD>
                </TR>
              ))}
            </TBody>
          </Table>
        )}
    </Card>
  )
}

function ModerationButton({ company }: { company: AdminCompany }) {
  const { suspend, restore } = useCompanyModeration()
  const [text, setText] = useState('')
  const suspended = company.status === 'SUSPENDED'

  return suspended ? (
    <Confirm
      trigger={<Button size="sm" variant="outline"><RotateCcw /> Restore</Button>}
      title={`Restore ${company.name}?`}
      description="Members get access back, public claim pages reappear and paused payouts continue."
      confirmLabel="Restore"
      onConfirm={() =>
        restore.mutateAsync({ id: company.id, note: text.trim() || undefined })
          .then(() => { toast.success(`${company.name} restored`); setText('') })
          .catch((e) => { toastError(e); throw e })
      }
    >
      <Textarea value={text} onChange={(e) => setText(e.target.value)} placeholder="Note for the audit log (optional)" maxLength={500} />
    </Confirm>
  ) : (
    <Confirm
      trigger={<Button size="sm" variant="outline" className="border-destructive/40 text-destructive hover:bg-destructive/10 hover:text-destructive"><Ban /> Suspend</Button>}
      title={`Suspend ${company.name}?`}
      description="Members are locked out, public claim pages go offline and payouts pause until you restore the company."
      confirmLabel="Suspend company"
      destructive
      onConfirm={() => {
        if (!text.trim()) {
          toast.error('Write a reason first')
          return Promise.reject(new Error('reason required'))
        }
        return suspend.mutateAsync({ id: company.id, reason: text.trim() })
          .then(() => { toast.success(`${company.name} suspended`); setText('') })
          .catch((e) => { toastError(e); throw e })
      }}
    >
      <Textarea value={text} onChange={(e) => setText(e.target.value)} placeholder="Reason (shown to the company and kept in the audit log)" maxLength={500} autoFocus />
    </Confirm>
  )
}
