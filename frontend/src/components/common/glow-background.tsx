import { cn } from '@/lib/utils'

/** Animated gradient glow + subtle grid, behind the public (glass) pages. */
export function GlowBackground({ className }: { className?: string }) {
  return (
    <div aria-hidden className={cn('pointer-events-none fixed inset-0 -z-10 overflow-hidden bg-background', className)}>
      <div className="absolute -left-40 -top-40 size-[38rem] rounded-full bg-violet-600/30 blur-[120px] animate-glow" />
      <div className="absolute -bottom-48 -right-32 size-[34rem] rounded-full bg-cyan-500/25 blur-[120px] animate-glow [animation-delay:-7s]" />
      <div className="absolute left-1/3 top-1/2 size-[22rem] rounded-full bg-fuchsia-500/10 blur-[100px] animate-glow [animation-delay:-3s]" />
      <div
        className="absolute inset-0 opacity-[0.15] dark:opacity-[0.08]"
        style={{
          backgroundImage:
            'linear-gradient(to right, currentColor 1px, transparent 1px), linear-gradient(to bottom, currentColor 1px, transparent 1px)',
          backgroundSize: '48px 48px',
          maskImage: 'radial-gradient(ellipse at center, black 30%, transparent 75%)',
        }}
      />
    </div>
  )
}
