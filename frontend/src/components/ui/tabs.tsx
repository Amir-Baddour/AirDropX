import * as TabsPrimitive from '@radix-ui/react-tabs'
import * as React from 'react'
import { cn } from '@/lib/utils'

export const Tabs = TabsPrimitive.Root
export const TabsContent = ({ className, ...p }: React.ComponentProps<typeof TabsPrimitive.Content>) => (
  <TabsPrimitive.Content className={cn('mt-5 outline-none', className)} {...p} />
)
export const TabsList = ({ className, ...p }: React.ComponentProps<typeof TabsPrimitive.List>) => (
  <TabsPrimitive.List className={cn('flex gap-1 overflow-x-auto border-b', className)} {...p} />
)
export const TabsTrigger = ({ className, ...p }: React.ComponentProps<typeof TabsPrimitive.Trigger>) => (
  <TabsPrimitive.Trigger
    className={cn(
      'relative -mb-px inline-flex items-center gap-2 whitespace-nowrap border-b-2 border-transparent px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground cursor-pointer data-[state=active]:border-primary data-[state=active]:text-foreground',
      className,
    )}
    {...p}
  />
)
