import { Fragment, useState } from 'react'
import { Link } from 'react-router'
import { Check, ChevronDown, ExternalLink, Inbox, Lock, Unlock, X } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Confirm } from '@/components/ui/confirm'
import { Input, Textarea } from '@/components/ui/input'
import { Field } from '@/components/ui/label'
import { CopyButton, EmptyState, Mono, Skeleton } from '@/components/ui/misc'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { ClaimStatusBadge } from '@/components/common/status'
import { ErrorBox, toastError } from '@/components/common/errors'
import { useClaims, useClaimSettings, useReviewClaim, useTasks } from '@/lib/queries'
import type { Airdrop, Claim, ClaimStatus, Task } from '@/lib/types'
import { cn, formatAmount, shortAddress, timeAgo } from '@/lib/utils'

const FILTERS: { value: ClaimStatus | ''; label: string }[] = [
  { value: 'NEEDS_REVIEW', label: 'Needs review' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: '', label: 'All' },
]

export default function ClaimsTab({ airdrop }: { airdrop: Airdrop }) {
  const [filter, setFilter] = useState<ClaimStatus | ''>('NEEDS_REVIEW')
  const claims = useClaims(airdrop.id, filter || undefined)
  const tasks = useTasks(airdrop.id)
  const counts = claims.data?.counts

  return (
    <div className="grid gap-4">
      <ClaimSettingsCard airdrop={airdrop} />
      <Card>
        <CardHeader className="gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>Review queue</CardTitle>
            <CardDescription>Approved claims become recipients. Rejected claimants see your reason.</CardDescription>
          </div>
          <div className="flex gap-1 overflow-x-auto">
            {FILTERS.map((f) => (
              <button
                key={f.label}
                onClick={() => setFilter(f.value)}
                className={cn('rounded-md px-2.5 py-1 text-xs font-medium whitespace-nowrap cursor-pointer', filter === f.value ? 'bg-accent' : 'text-muted-foreground hover:text-foreground')}
              >
                {f.label}
                {f.value && counts && <span className="ml-1.5 text-muted-foreground">{counts[f.value]}</span>}
              </button>
            ))}
          </div>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {claims.isLoading ? <div className="p-5 pt-0"><Skeleton className="h-24" /></div>
            : claims.error ? <ErrorBox error={claims.error} className="m-5 mt-0" />
            : !claims.data?.items.length ? (
              <EmptyState
                icon={<Inbox />}
                title={filter === 'NEEDS_REVIEW' ? 'Nothing to review' : 'No claims'}
                description={filter === 'NEEDS_REVIEW' ? 'Claims with manual-proof tasks will wait here for you.' : undefined}
                className="m-5 mt-0"
              />
            ) : <ClaimTable airdrop={airdrop} claims={claims.data.items} tasks={tasks.data ?? []} />}
        </CardContent>
      </Card>
    </div>
  )
}

function ClaimSettingsCard({ airdrop }: { airdrop: Airdrop }) {
  const { open, close } = useClaimSettings(airdrop.id)
  const [amount, setAmount] = useState(airdrop.claim_amount ? formatAmount(airdrop.claim_amount, 18).replace(/,/g, '') : '')
  const [max, setMax] = useState(airdrop.max_claims?.toString() ?? '')
  const publicUrl = `${window.location.origin}/claim/${airdrop.id}`
  const draft = airdrop.status === 'DRAFT'

  if (airdrop.claims_open) {
    return (
      <Card className="border-success/30">
        <CardHeader className="gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <CardTitle className="flex items-center gap-2"><span className="size-2 rounded-full bg-success animate-pulse-dot" /> Claims are open</CardTitle>
            <CardDescription>
              {formatAmount(airdrop.claim_amount)} {airdrop.token_symbol} per claim{airdrop.max_claims ? ` · limit ${airdrop.max_claims}` : ' · no limit'}
            </CardDescription>
          </div>
          <Confirm
            trigger={<Button variant="outline" size="sm"><Lock /> Close claims</Button>}
            title="Close public claims?"
            description="The public page stops accepting claims. Existing claims stay as they are."
            confirmLabel="Close claims"
            onConfirm={() => close.mutateAsync().then(() => toast.success('Claims closed')).catch((e) => { toastError(e); throw e })}
          />
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2 rounded-md border bg-muted/40 px-3 py-2">
            <span className="min-w-0 flex-1 truncate font-mono text-xs">{publicUrl}</span>
            <CopyButton value={publicUrl} label="Link copied" />
            <Button variant="ghost" size="sm" asChild><Link to={`/claim/${airdrop.id}`} target="_blank"><ExternalLink /> Open</Link></Button>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Public claim page</CardTitle>
        <CardDescription>
          {draft ? 'Let anyone claim a fixed amount by completing your tasks. No login needed for claimants.' : 'Claims can only be opened while the airdrop is a draft.'}
        </CardDescription>
      </CardHeader>
      {draft && (
        <CardContent>
          <form
            className="flex flex-wrap items-end gap-3"
            onSubmit={(e) => {
              e.preventDefault()
              if (!/^\d+(\.\d{1,18})?$/.test(amount) || Number(amount) <= 0) return toast.error('Enter a positive amount (max 18 decimals)')
              if (max && !/^\d+$/.test(max)) return toast.error('Limit must be a whole number')
              open.mutateAsync({ claim_amount: amount, max_claims: max ? Number(max) : null })
                .then(() => toast.success('Claims opened', { description: 'Share the public link.' }))
                .catch(toastError)
            }}
          >
            <Field label={`Amount per claim (${airdrop.token_symbol})`} className="w-48">
              <Input value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="25" inputMode="decimal" />
            </Field>
            <Field label="Max claims (optional)" className="w-44">
              <Input value={max} onChange={(e) => setMax(e.target.value)} placeholder="No limit" inputMode="numeric" />
            </Field>
            <Button type="submit" loading={open.isPending}><Unlock /> Open claims</Button>
          </form>
        </CardContent>
      )}
    </Card>
  )
}

function ClaimTable({ airdrop, claims, tasks }: { airdrop: Airdrop; claims: Claim[]; tasks: Task[] }) {
  const [expanded, setExpanded] = useState<string | null>(null)
  const canReview = airdrop.status === 'DRAFT'
  const taskById = new Map(tasks.map((t) => [t.id, t]))
  return (
    <Table>
      <THead>
        <TR className="hover:bg-transparent"><TH className="w-8" /><TH>Wallet</TH><TH>Status</TH><TH className="hidden sm:table-cell">Submitted</TH><TH className="text-right">Actions</TH></TR>
      </THead>
      <TBody>
        {claims.map((c) => {
          const isOpen = expanded === c.id
          return (
            <Fragment key={c.id}>
              <TR className="cursor-pointer" onClick={() => setExpanded(isOpen ? null : c.id)}>
                <TD><ChevronDown className={cn('size-4 text-muted-foreground transition-transform', isOpen && 'rotate-180')} /></TD>
                <TD onClick={(e) => e.stopPropagation()}><Mono value={c.address} display={shortAddress(c.address, 8, 6)} /></TD>
                <TD><ClaimStatusBadge status={c.status} /></TD>
                <TD className="hidden text-muted-foreground sm:table-cell">{timeAgo(c.created_at)}</TD>
                <TD className="text-right" onClick={(e) => e.stopPropagation()}>
                  {c.status === 'NEEDS_REVIEW' && canReview ? <ReviewButtons airdropId={airdrop.id} claim={c} /> : <span className="text-xs text-muted-foreground">{c.reviewed_at ? `reviewed ${timeAgo(c.reviewed_at)}` : '—'}</span>}
                </TD>
              </TR>
              {isOpen && (
                <TR className="bg-muted/30 hover:bg-muted/30">
                  <TD />
                  <TD colSpan={4}>
                    <ul className="grid gap-2 py-1">
                      {c.results.map((r) => {
                        const t = taskById.get(r.task_id)
                        return (
                          <li key={r.task_id} className="flex flex-wrap items-start gap-2 text-sm">
                            {r.needs_review ? <span className="mt-0.5 size-4 rounded-full border-2 border-warning" /> : r.passed ? <Check className="mt-0.5 size-4 text-success" /> : <X className="mt-0.5 size-4 text-destructive" />}
                            <span className="font-medium">{t?.title ?? 'Task'}</span>
                            {r.proof && (
                              /^https?:\/\//.test(r.proof)
                                ? <a href={r.proof} target="_blank" rel="noopener noreferrer nofollow" className="break-all text-primary hover:underline">{r.proof}</a>
                                : <span className="break-all text-muted-foreground">“{r.proof}”</span>
                            )}
                            {!r.proof && r.detail && <span className="text-muted-foreground">{r.detail}</span>}
                          </li>
                        )
                      })}
                      {c.results.length === 0 && <li className="text-sm text-muted-foreground">No tasks: approved on a valid wallet.</li>}
                      {c.reject_reason && <li className="text-sm text-destructive">Rejected: {c.reject_reason}</li>}
                    </ul>
                  </TD>
                </TR>
              )}
            </Fragment>
          )
        })}
      </TBody>
    </Table>
  )
}

function ReviewButtons({ airdropId, claim }: { airdropId: string; claim: Claim }) {
  const { approve, reject } = useReviewClaim(airdropId)
  const [reason, setReason] = useState('')
  return (
    <div className="inline-flex gap-1.5">
      <Button
        size="sm"
        variant="outline"
        className="border-success/40 text-success hover:bg-success/10 hover:text-success"
        loading={approve.isPending}
        onClick={() => approve.mutateAsync(claim.id).then(() => toast.success('Claim approved', { description: 'Added as a recipient.' })).catch(toastError)}
      >
        <Check /> Approve
      </Button>
      <Confirm
        trigger={<Button size="sm" variant="outline" className="border-destructive/40 text-destructive hover:bg-destructive/10 hover:text-destructive"><X /> Reject</Button>}
        title="Reject this claim?"
        description={<>The claimant ({shortAddress(claim.address)}) will see your reason on their status page.</>}
        confirmLabel="Reject"
        destructive
        onConfirm={() => {
          if (!reason.trim()) {
            toast.error('Write a reason first')
            return Promise.reject(new Error('reason required'))
          }
          return reject.mutateAsync({ claimId: claim.id, reason: reason.trim() }).then(() => { toast.success('Claim rejected'); setReason('') }).catch((e) => { toastError(e); throw e })
        }}
      >
        <Textarea value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. The linked post was deleted" maxLength={500} autoFocus />
      </Confirm>
    </div>
  )
}
