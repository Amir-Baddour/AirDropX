import { Link } from 'react-router'
import { ArrowLeft } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { GlowBackground } from '@/components/common/glow-background'
import { Logo } from '@/components/common/logo'

const STEPS = [
  ['Create an airdrop', 'Give it a name and a token symbol. It starts as a draft.'],
  ['Choose who gets paid', 'Upload a list of wallets and amounts, or add tasks and open a public claim page, or both.'],
  ['Add tasks', 'Quiz and secret code are verified instantly. Manual proof waits for your review.'],
  ['Open claims', 'Set the amount per claim and an optional limit. Share the public link.'],
  ['Review', 'Approve or reject proof-based claims. Approved claimants become recipients.'],
  ['Validate', 'Freezes the recipient list and closes claims. You can still move back to draft.'],
  ['Launch', 'The worker sends payouts in batches. Claimants watch their status live.'],
]

export default function DocsPage() {
  return (
    <div className="relative min-h-dvh">
      <GlowBackground />
      <div className="mx-auto max-w-3xl px-4 py-8">
        <div className="flex items-center justify-between">
          <Logo />
          <Link to="/" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"><ArrowLeft className="size-4" /> Home</Link>
        </div>
        <h1 className="mt-12 text-3xl font-semibold tracking-tight">How AirdropX works</h1>
        <p className="mt-2 text-muted-foreground">From an idea to verified payouts in seven steps.</p>
        <ol className="mt-8 grid gap-3">
          {STEPS.map(([title, text], i) => (
            <Card key={title} className="glass flex gap-4 p-5">
              <span className="flex size-8 shrink-0 items-center justify-center rounded-full brand-gradient text-sm font-semibold text-white">{i + 1}</span>
              <div>
                <p className="font-medium">{title}</p>
                <p className="text-sm text-muted-foreground">{text}</p>
              </div>
            </Card>
          ))}
        </ol>
      </div>
    </div>
  )
}
