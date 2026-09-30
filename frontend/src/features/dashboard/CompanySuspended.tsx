import { ShieldAlert } from 'lucide-react'

export default function CompanySuspended({ reason }: { reason: string | null }) {
  return (
    <div className="mx-auto max-w-md pt-16 text-center">
      <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive"><ShieldAlert /></div>
      <h1 className="mt-4 text-xl font-semibold">Your company is suspended</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        A platform admin paused this company. Your public claim pages are offline and payouts are on hold until it is restored.
      </p>
      {reason && <p className="mt-4 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm">Reason: {reason}</p>}
    </div>
  )
}
