import { Check, X } from 'lucide-react'
import type { Airdrop, AirdropStatus } from '@/lib/types'
import { cn } from '@/lib/utils'

const STEPS: { status: AirdropStatus; label: string; hint: string }[] = [
  { status: 'DRAFT', label: 'Draft', hint: 'Add recipients, tasks, open claims' },
  { status: 'VALIDATED', label: 'Validated', hint: 'List frozen, ready to launch' },
  { status: 'PROCESSING', label: 'Processing', hint: 'Worker is sending payouts' },
  { status: 'COMPLETED', label: 'Completed', hint: 'All payouts finished' },
]

export default function LifecycleStepper({ airdrop }: { airdrop: Airdrop }) {
  const cancelled = airdrop.status === 'CANCELLED'
  const current = STEPS.findIndex((s) => s.status === airdrop.status)
  return (
    <ol className="grid grid-cols-2 gap-3 rounded-lg border bg-card p-4 sm:grid-cols-4">
      {STEPS.map((step, i) => {
        const done = !cancelled && (i < current || airdrop.status === 'COMPLETED')
        const active = !cancelled && i === current && airdrop.status !== 'COMPLETED'
        return (
          <li key={step.status} className="flex items-start gap-3">
            <span
              className={cn(
                'mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-full border text-xs font-semibold',
                done && 'border-transparent bg-success text-white',
                active && 'border-transparent brand-gradient text-white shadow-md shadow-primary/30',
                !done && !active && 'text-muted-foreground',
              )}
            >
              {done ? <Check className="size-3.5" /> : i + 1}
            </span>
            <div className="min-w-0">
              <p className={cn('text-sm font-medium', !done && !active && 'text-muted-foreground')}>{step.label}</p>
              <p className="text-xs text-muted-foreground">{step.hint}</p>
            </div>
          </li>
        )
      })}
      {cancelled && (
        <li className="col-span-full flex items-center gap-2 text-sm text-destructive"><X className="size-4" /> This airdrop was cancelled.</li>
      )}
    </ol>
  )
}
