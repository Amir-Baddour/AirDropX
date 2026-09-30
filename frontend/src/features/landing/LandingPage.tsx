import { Link } from 'react-router'
import { motion } from 'motion/react'
import { ArrowRight, BadgeCheck, BrainCircuit, Gauge, KeyRound, ShieldCheck, Workflow } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { GlowBackground } from '@/components/common/glow-background'
import { Logo } from '@/components/common/logo'
import { ThemeToggle } from '@/components/common/theme-toggle'
import { useSession } from '@/lib/auth'

const FEATURES = [
  { icon: BrainCircuit, title: 'Verified tasks', text: 'Quizzes and secret codes are checked instantly. Proof tasks go to a review queue.' },
  { icon: ShieldCheck, title: 'Built against bots', text: 'One claim per wallet, per-network limits, rate limiting and hard claim caps.' },
  { icon: Workflow, title: 'Safe lifecycle', text: 'Draft → validated → processing → completed, with every step on a timeline.' },
  { icon: Gauge, title: 'Live payouts', text: 'A background worker sends payouts in batches and retries failures.' },
  { icon: KeyRound, title: 'No login for claimants', text: 'People claim with a wallet and get a private link to track their payout.' },
  { icon: BadgeCheck, title: 'Multi-tenant', text: "Each company only ever sees its own airdrops, tasks and claims." },
]

export default function LandingPage() {
  const { isAuthenticated } = useSession()
  return (
    <div className="relative min-h-dvh">
      <GlowBackground />
      <header className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5">
        <Logo />
        <div className="flex items-center gap-2">
          <Button variant="ghost" asChild className="hidden sm:inline-flex"><Link to="/docs">How it works</Link></Button>
          <ThemeToggle />
          <Button asChild><Link to={isAuthenticated ? '/app' : '/login'}>{isAuthenticated ? 'Dashboard' : 'Sign in'}</Link></Button>
        </div>
      </header>

      <section className="mx-auto max-w-4xl px-4 pb-20 pt-16 text-center sm:pt-24">
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>
          <span className="glass inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs text-muted-foreground">
            <span className="size-1.5 rounded-full bg-success animate-pulse-dot" /> Verified reach, not just distribution
          </span>
          <h1 className="mt-6 text-4xl font-semibold tracking-tight sm:text-6xl">
            Airdrops that reach <span className="brand-text">real people</span>
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg text-muted-foreground">
            Create a campaign, add tasks people must complete, review claims and send payouts, all from one dashboard.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Button variant="brand" size="lg" asChild><Link to={isAuthenticated ? '/app' : '/login'}>Start an airdrop <ArrowRight /></Link></Button>
            <Button variant="outline" size="lg" asChild><Link to="/docs">See how it works</Link></Button>
          </div>
        </motion.div>
      </section>

      <section className="mx-auto grid max-w-6xl gap-4 px-4 pb-24 sm:grid-cols-2 lg:grid-cols-3">
        {FEATURES.map((f, i) => (
          <motion.div
            key={f.title}
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: i * 0.05 }}
            className="glass rounded-2xl p-6"
          >
            <f.icon className="size-6 text-primary" />
            <h3 className="mt-4 font-semibold">{f.title}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{f.text}</p>
          </motion.div>
        ))}
      </section>

      <footer className="border-t py-8 text-center text-xs text-muted-foreground">
        AirdropX · Web2 demo platform · payouts are simulated
      </footer>
    </div>
  )
}
