import { useParams } from 'react-router'
import { Check, Clock, FileCheck2, Send, X } from 'lucide-react'
import { Mono, Skeleton } from '@/components/ui/misc'
import { usePublicClaim } from '@/lib/queries'
import type { PublicClaimStatus } from '@/lib/types'
import { cn, shortAddress } from '@/lib/utils'
import PublicShell from './PublicShell'

type StepState = 'done' | 'active' | 'todo' | 'failed'

function stepsFor(s: PublicClaimStatus): { label: string; detail: string; state: StepState; icon: typeof Check }[] {
  const rejected = s.status === 'REJECTED'
  const approved = s.status === 'APPROVED'
  const p = s.payout_status
  return [
    { label: 'Submitted', detail: 'Your tasks were received', state: 'done', icon: FileCheck2 },
    {
      label: rejected ? 'Rejected' : approved ? 'Approved' : 'In review',
      detail: rejected ? s.reject_reason ?? 'Your claim was rejected' : approved ? 'You are on the recipient list' : 'The team is checking your proof',
      state: rejected ? 'failed' : approved ? 'done' : 'active',
      icon: rejected ? X : approved ? Check : Clock,
    },
    {
      label: p === 'COMPLETED' ? 'Paid' : p === 'FAILED' ? 'Payout failed' : p === 'CANCELLED' ? 'Cancelled' : p === 'PROCESSING' ? 'Sending' : 'Waiting for launch',
      detail: p === 'COMPLETED' ? 'Tokens sent to your wallet' : p === 'FAILED' ? 'The team will retry' : p === 'CANCELLED' ? 'The airdrop was cancelled' : p === 'PROCESSING' ? 'Your payout is being sent' : 'Payouts start when the team launches',
      state: rejected ? 'todo' : p === 'COMPLETED' ? 'done' : p === 'FAILED' || p === 'CANCELLED' ? 'failed' : approved ? 'active' : 'todo',
      icon: p === 'COMPLETED' ? Check : Send,
    },
  ]
}

export default function StatusPage() {
  const { token = '' } = useParams()
  const { data, isLoading, error } = usePublicClaim(token)

  return (
    <PublicShell>
      <div className="glass rounded-2xl p-6 sm:p-8">
        {isLoading ? (
          <div className="space-y-4"><Skeleton className="h-6 w-48" /><Skeleton className="h-40 w-full" /></div>
        ) : error || !data ? (
          <div className="py-6 text-center">
            <p className="text-lg font-semibold">Claim not found</p>
            <p className="mt-1 text-sm text-muted-foreground">Check that you copied the full status link.</p>
          </div>
        ) : (
          <>
            <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">Claim status</p>
            <h1 className="mt-2 text-2xl font-semibold tracking-tight">{data.airdrop_name}</h1>
            <p className="mt-1 text-sm text-muted-foreground">Wallet <span className="font-mono">{shortAddress(data.address, 8, 6)}</span> · <span className="font-mono">{data.token_symbol}</span></p>

            <ol className="mt-8 space-y-0">
              {stepsFor(data).map((s, i, all) => (
                <li key={s.label} className="relative flex gap-4 pb-7 last:pb-0">
                  {i < all.length - 1 && <span className={cn('absolute left-[17px] top-9 h-[calc(100%-2.25rem)] w-0.5', s.state === 'done' ? 'bg-success' : 'bg-foreground/10')} />}
                  <span
                    className={cn(
                      'z-10 flex size-9 shrink-0 items-center justify-center rounded-full border [&_svg]:size-4',
                      s.state === 'done' && 'border-transparent bg-success text-white',
                      s.state === 'active' && 'border-transparent brand-gradient text-white animate-pulse-dot',
                      s.state === 'failed' && 'border-transparent bg-destructive text-white',
                      s.state === 'todo' && 'border-foreground/15 text-muted-foreground',
                    )}
                  >
                    <s.icon />
                  </span>
                  <div className="pt-1.5">
                    <p className={cn('font-medium', s.state === 'todo' && 'text-muted-foreground')}>{s.label}</p>
                    <p className={cn('text-sm text-muted-foreground', s.state === 'failed' && 'text-destructive')}>{s.detail}</p>
                  </div>
                </li>
              ))}
            </ol>

            {data.tx_ref && (
              <div className="mt-8 rounded-lg border border-success/30 bg-success/10 p-4">
                <p className="text-xs text-muted-foreground">Transaction (simulated)</p>
                <Mono value={data.tx_ref} display={shortAddress(data.tx_ref, 14, 10)} className="mt-1 text-sm" />
              </div>
            )}
            <p className="mt-6 text-center text-xs text-muted-foreground">This page updates automatically.</p>
          </>
        )}
      </div>
    </PublicShell>
  )
}
