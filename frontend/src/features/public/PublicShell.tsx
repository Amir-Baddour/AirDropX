import type { ReactNode } from 'react'
import { GlowBackground } from '@/components/common/glow-background'
import { Logo } from '@/components/common/logo'
import { ThemeToggle } from '@/components/common/theme-toggle'

export default function PublicShell({ children }: { children: ReactNode }) {
  return (
    <div className="relative flex min-h-dvh flex-col">
      <GlowBackground />
      <header className="mx-auto flex w-full max-w-5xl items-center justify-between px-4 py-5">
        <Logo />
        <ThemeToggle />
      </header>
      <main className="mx-auto flex w-full max-w-xl flex-1 flex-col px-4 pb-16 pt-4">{children}</main>
      <footer className="pb-6 text-center text-xs text-muted-foreground">
        Powered by AirdropX · payouts on this demo are simulated
      </footer>
    </div>
  )
}
