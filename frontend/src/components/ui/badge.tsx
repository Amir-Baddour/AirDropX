import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const badgeVariants = cva('inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-xs font-medium whitespace-nowrap', {
  variants: {
    tone: {
      neutral: 'border-border bg-muted text-muted-foreground',
      info: 'border-info/30 bg-info/10 text-info',
      warning: 'border-warning/30 bg-warning/10 text-warning',
      success: 'border-success/30 bg-success/10 text-success',
      danger: 'border-destructive/30 bg-destructive/10 text-destructive',
      brand: 'border-primary/30 bg-primary/10 text-primary',
    },
  },
  defaultVariants: { tone: 'neutral' },
})

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {
  dot?: boolean
  pulse?: boolean
}

export function Badge({ className, tone, dot, pulse, children, ...props }: BadgeProps) {
  return (
    <span className={cn(badgeVariants({ tone }), className)} {...props}>
      {dot && <span className={cn('size-1.5 rounded-full bg-current', pulse && 'animate-pulse-dot')} />}
      {children}
    </span>
  )
}
