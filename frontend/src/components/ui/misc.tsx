import * as ProgressPrimitive from '@radix-ui/react-progress'
import * as React from 'react'
import { Check, Copy } from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'

export const Skeleton = ({ className, ...p }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn('animate-pulse rounded-md bg-muted', className)} {...p} />
)

export function Progress({ value, className, indicatorClassName }: { value: number; className?: string; indicatorClassName?: string }) {
  return (
    <ProgressPrimitive.Root className={cn('relative h-2 w-full overflow-hidden rounded-full bg-muted', className)} value={value}>
      <ProgressPrimitive.Indicator
        className={cn('h-full brand-gradient transition-transform duration-700 ease-out', indicatorClassName)}
        style={{ transform: `translateX(-${100 - Math.min(100, Math.max(0, value))}%)` }}
      />
    </ProgressPrimitive.Root>
  )
}

export function EmptyState({ icon, title, description, action, className }: {
  icon?: React.ReactNode; title: string; description?: React.ReactNode; action?: React.ReactNode; className?: string
}) {
  return (
    <div className={cn('flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed px-6 py-12 text-center', className)}>
      {icon && <div className="flex size-11 items-center justify-center rounded-full bg-muted text-muted-foreground [&_svg]:size-5">{icon}</div>}
      <div className="space-y-1">
        <p className="font-medium">{title}</p>
        {description && <p className="mx-auto max-w-sm text-sm text-muted-foreground">{description}</p>}
      </div>
      {action}
    </div>
  )
}

export function CopyButton({ value, label = 'Copied', className }: { value: string; label?: string; className?: string }) {
  const [done, setDone] = React.useState(false)
  return (
    <button
      type="button"
      className={cn('inline-flex size-7 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground cursor-pointer', className)}
      onClick={async () => {
        try {
          await navigator.clipboard.writeText(value)
          setDone(true)
          toast.success(label)
          setTimeout(() => setDone(false), 1500)
        } catch {
          toast.error('Could not copy. Select the text and copy it manually.')
        }
      }}
      aria-label="Copy"
    >
      {done ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
    </button>
  )
}

/** Monospace value (address, tx hash) with copy. */
export function Mono({ value, display, className }: { value: string; display?: string; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1 font-mono text-xs', className)} title={value}>
      {display ?? value}
      <CopyButton value={value} />
    </span>
  )
}
