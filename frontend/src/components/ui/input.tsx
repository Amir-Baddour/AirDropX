import * as React from 'react'
import { cn } from '@/lib/utils'

const base =
  'w-full rounded-md border border-input bg-transparent px-3 text-sm shadow-xs transition-colors placeholder:text-muted-foreground/70 outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:opacity-50 aria-[invalid=true]:border-destructive aria-[invalid=true]:ring-destructive/20'

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => <input ref={ref} className={cn(base, 'h-9', className)} {...props} />,
)
Input.displayName = 'Input'

export const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => <textarea ref={ref} className={cn(base, 'min-h-20 py-2', className)} {...props} />,
)
Textarea.displayName = 'Textarea'

export const NativeSelect = React.forwardRef<HTMLSelectElement, React.SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, ...props }, ref) => <select ref={ref} className={cn(base, 'h-9 bg-card', className)} {...props} />,
)
NativeSelect.displayName = 'NativeSelect'
