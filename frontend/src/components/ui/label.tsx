import * as LabelPrimitive from '@radix-ui/react-label'
import * as React from 'react'
import { cn } from '@/lib/utils'

export const Label = ({ className, ...props }: React.ComponentProps<typeof LabelPrimitive.Root>) => (
  <LabelPrimitive.Root className={cn('text-sm font-medium leading-none select-none', className)} {...props} />
)

/** Label + control + error/hint, used by every form. */
export function Field({ label, htmlFor, error, hint, children, className }: {
  label: string; htmlFor?: string; error?: string; hint?: React.ReactNode; children: React.ReactNode; className?: string
}) {
  return (
    <div className={cn('grid gap-1.5', className)}>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {error ? <p className="text-xs text-destructive">{error}</p> : hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
    </div>
  )
}
