import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router'
import { AnimatePresence, motion } from 'motion/react'
import { AlertTriangle, ArrowLeft, ArrowRight, BrainCircuit, Check, FileCheck2, Gift, KeyRound, Wallet } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input, Textarea } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { CopyButton, Skeleton } from '@/components/ui/misc'
import { ApiError } from '@/lib/api'
import { usePublicAirdrop, useSubmitClaim } from '@/lib/queries'
import type { ClaimSubmitted, PublicAirdrop, Task } from '@/lib/types'
import { cn, formatAmount, isValidAddress } from '@/lib/utils'
import PublicShell from './PublicShell'
import { Linkify } from '@/components/common/linkify'

type Answers = Record<string, unknown>

const ICONS = { QUIZ: BrainCircuit, SECRET_CODE: KeyRound, MANUAL_PROOF: FileCheck2 }

export default function ClaimPage() {
  const { id = '' } = useParams()
  const { data, isLoading, error } = usePublicAirdrop(id)

  return (
    <PublicShell>
      {isLoading ? (
        <div className="glass space-y-4 rounded-2xl p-8"><Skeleton className="h-6 w-40" /><Skeleton className="h-10 w-full" /><Skeleton className="h-32 w-full" /></div>
      ) : error || !data ? (
        <div className="glass rounded-2xl p-8 text-center">
          <Gift className="mx-auto size-10 text-muted-foreground" />
          <p className="mt-4 text-lg font-semibold">This airdrop isn't accepting claims</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {error instanceof ApiError && error.status === 404 ? 'It may have ended or not started yet.' : 'Please try again in a moment.'}
          </p>
        </div>
      ) : (
        <ClaimFlow airdrop={data} />
      )}
    </PublicShell>
  )
}

function ClaimFlow({ airdrop }: { airdrop: PublicAirdrop }) {
  const steps = useMemo(() => [...airdrop.tasks.map((t) => ({ kind: 'task' as const, task: t })), { kind: 'wallet' as const }], [airdrop.tasks])
  const [step, setStep] = useState(-1) // -1 = intro
  const [answers, setAnswers] = useState<Answers>({})
  const [address, setAddress] = useState('')
  const [result, setResult] = useState<ClaimSubmitted | null>(null)
  const [serverErrors, setServerErrors] = useState<{ message: string; list: string[] } | null>(null)
  const submit = useSubmitClaim(airdrop.id)

  const current = steps[step]
  const taskDone = (t: Task) => {
    const a = answers[t.id]
    if (t.type === 'QUIZ') return Array.isArray(a) && a.length === (t.config.questions?.length ?? 0) && a.every((x) => x !== undefined && x !== null)
    return typeof a === 'string' && a.trim().length > 0
  }
  const canNext = step === -1 || (current?.kind === 'task' ? taskDone(current.task) : isValidAddress(address))

  const send = async () => {
    setServerErrors(null)
    try {
      setResult(await submit.mutateAsync({ address: address.trim(), answers }))
    } catch (e) {
      if (e instanceof ApiError) {
        setServerErrors({ message: e.message, list: e.errors })
        // Wrong answers: jump back to the first task so the claimant can fix it
        if (e.status === 422 && airdrop.tasks.length) setStep(0)
      } else setServerErrors({ message: 'Something went wrong. Please try again.', list: [] })
    }
  }

  if (result) return <Success airdrop={airdrop} result={result} />

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="glass overflow-hidden rounded-2xl">
      {/* progress */}
      {step >= 0 && (
        <div className="flex gap-1 px-6 pt-6">
          {steps.map((_, i) => <div key={i} className={cn('h-1 flex-1 rounded-full transition-colors', i <= step ? 'brand-gradient' : 'bg-foreground/10')} />)}
        </div>
      )}

      <div className="p-6 sm:p-8">
        <AnimatePresence mode="wait">
          <motion.div key={step} initial={{ opacity: 0, x: 16 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -16 }} transition={{ duration: 0.18 }}>
            {step === -1 && <Intro airdrop={airdrop} />}
            {current?.kind === 'task' && (
              <TaskStep task={current.task} index={step} total={airdrop.tasks.length} value={answers[current.task.id]} onChange={(v) => { setServerErrors(null); setAnswers((a) => ({ ...a, [current.task.id]: v })) }} />
            )}
            {current?.kind === 'wallet' && (
              <div>
                <StepTitle icon={<Wallet />} eyebrow="Last step" title="Where should we send your tokens?" />
                <Label htmlFor="wallet" className="mt-6 block">Wallet address</Label>
                <Input
                  id="wallet"
                  value={address}
                  onChange={(e) => { setServerErrors(null); setAddress(e.target.value) }}
                  placeholder="0x… or Solana address"
                  className="mt-2 h-11 font-mono text-sm"
                  autoFocus
                  aria-invalid={address.length > 0 && !isValidAddress(address)}
                />
                <p className={cn('mt-2 text-xs', address && !isValidAddress(address) ? 'text-destructive' : 'text-muted-foreground')}>
                  {address && !isValidAddress(address) ? 'That doesn\'t look like an EVM (0x…) or Solana address.' : 'One claim per wallet. Double-check it; payouts can\'t be redirected.'}
                </p>
                <p className="mt-3 text-xs text-muted-foreground">
                  No wallet yet?{' '}
                  <a href="https://metamask.io/download/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Get MetaMask</a>
                  {' '}(free, about a minute), then paste your Ethereum address here.
                </p>
              </div>
            )}
          </motion.div>
        </AnimatePresence>

        {serverErrors && (
          <div className="mt-6 flex gap-3 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm">
            <AlertTriangle className="mt-0.5 size-4 shrink-0 text-destructive" />
            <div>
              <p className="font-medium text-destructive">{serverErrors.message}</p>
              {serverErrors.list.map((e, i) => <p key={i} className="text-destructive/90">{e}</p>)}
            </div>
          </div>
        )}

        <div className="mt-8 flex items-center justify-between gap-3">
          {step >= 0 ? <Button variant="ghost" onClick={() => setStep((s) => s - 1)}><ArrowLeft /> Back</Button> : <span />}
          {current?.kind === 'wallet' ? (
            <Button variant="brand" size="lg" disabled={!canNext} loading={submit.isPending} onClick={send}>
              Claim {formatAmount(airdrop.claim_amount)} {airdrop.token_symbol}
            </Button>
          ) : (
            <Button variant={step === -1 ? 'brand' : 'default'} size="lg" disabled={!canNext} onClick={() => setStep((s) => s + 1)}>
              {step === -1 ? 'Start' : 'Continue'} <ArrowRight />
            </Button>
          )}
        </div>
      </div>
    </motion.div>
  )
}

function Intro({ airdrop }: { airdrop: PublicAirdrop }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">Airdrop</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">{airdrop.name}</h1>
      {airdrop.description && <p className="mt-2 text-muted-foreground"><Linkify text={airdrop.description} /></p>}
      <div className="mt-6 rounded-xl border border-foreground/10 bg-foreground/[.03] p-5 text-center">
        <p className="text-xs text-muted-foreground">You can earn</p>
        <p className="mt-1 text-4xl font-semibold tracking-tight">
          <span className="brand-text">{formatAmount(airdrop.claim_amount)}</span> <span className="font-mono text-2xl">{airdrop.token_symbol}</span>
        </p>
      </div>
      <p className="mt-6 text-sm font-medium">{airdrop.tasks.length ? `Complete ${airdrop.tasks.length} task${airdrop.tasks.length > 1 ? 's' : ''}:` : 'No tasks: just enter your wallet.'}</p>
      <ul className="mt-3 grid gap-2">
        {airdrop.tasks.map((t) => {
          const Icon = ICONS[t.type]
          return (
            <li key={t.id} className="flex items-center gap-3 rounded-lg border border-foreground/10 px-3 py-2.5 text-sm">
              <Icon className="size-4 text-primary" /> {t.title}
              <span className="ml-auto text-xs text-muted-foreground">{t.auto_verified ? 'instant check' : 'reviewed by team'}</span>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

function StepTitle({ icon, eyebrow, title, description }: { icon: React.ReactNode; eyebrow: string; title: string; description?: string | null }) {
  return (
    <div className="flex gap-4">
      <div className="flex size-11 shrink-0 items-center justify-center rounded-xl brand-gradient text-white [&_svg]:size-5">{icon}</div>
      <div>
        <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">{eyebrow}</p>
        <h2 className="text-xl font-semibold tracking-tight">{title}</h2>
        {description && <p className="mt-1 text-sm text-muted-foreground"><Linkify text={description} /></p>}
      </div>
    </div>
  )
}

function TaskStep({ task, index, total, value, onChange }: { task: Task; index: number; total: number; value: unknown; onChange: (v: unknown) => void }) {
  const Icon = ICONS[task.type]
  const eyebrow = `Task ${index + 1} of ${total}`
  if (task.type === 'QUIZ') {
    const chosen = (Array.isArray(value) ? value : []) as (number | null)[]
    return (
      <div>
        <StepTitle icon={<Icon />} eyebrow={eyebrow} title={task.title} description={task.description} />
        <div className="mt-6 space-y-6">
          {task.config.questions?.map((q, qi) => (
            <fieldset key={qi}>
              <legend className="mb-3 font-medium">{q.question}</legend>
              <div className="grid gap-2">
                {q.options.map((o, oi) => (
                  <button
                    key={oi}
                    type="button"
                    onClick={() => onChange(Array.from({ length: task.config.questions?.length ?? 0 }, (_, k) => (k === qi ? oi : chosen[k] ?? null)))}
                    className={cn(
                      'flex items-center gap-3 rounded-lg border border-foreground/10 px-4 py-3 text-left text-sm transition-all cursor-pointer hover:border-primary/50 hover:bg-primary/5',
                      chosen[qi] === oi && 'border-primary bg-primary/10 ring-1 ring-primary',
                    )}
                  >
                    <span className={cn('flex size-5 shrink-0 items-center justify-center rounded-full border', chosen[qi] === oi && 'border-primary bg-primary text-white')}>
                      {chosen[qi] === oi && <Check className="size-3" />}
                    </span>
                    {o}
                  </button>
                ))}
              </div>
            </fieldset>
          ))}
        </div>
      </div>
    )
  }
  if (task.type === 'SECRET_CODE') {
    return (
      <div>
        <StepTitle icon={<Icon />} eyebrow={eyebrow} title={task.title} description={task.description} />
        {task.config.hint && <p className="mt-5 rounded-lg border border-foreground/10 bg-foreground/[.03] px-4 py-3 text-sm"><span className="text-muted-foreground">Hint:</span> {task.config.hint}</p>}
        <Label htmlFor="code" className="mt-6 block">Secret code</Label>
        <Input id="code" value={(value as string) ?? ''} onChange={(e) => onChange(e.target.value)} placeholder="Enter the code" className="mt-2 h-11 font-mono uppercase tracking-widest" autoFocus />
      </div>
    )
  }
  return (
    <div>
      <StepTitle icon={<Icon />} eyebrow={eyebrow} title={task.title} description={task.description} />
      <p className="mt-5 rounded-lg border border-foreground/10 bg-foreground/[.03] px-4 py-3 text-sm"><Linkify text={task.config.instructions ?? ''} /></p>
      <Label htmlFor="proof" className="mt-6 block">Your proof</Label>
      <Textarea id="proof" value={(value as string) ?? ''} onChange={(e) => onChange(e.target.value)} placeholder="Type your answer or paste a link" className="mt-2" maxLength={500} autoFocus />
      <p className="mt-2 text-xs text-muted-foreground">The team reviews this by hand. You'll see the result on your status page.</p>
    </div>
  )
}

function Success({ airdrop, result }: { airdrop: PublicAirdrop; result: ClaimSubmitted }) {
  const statusUrl = `${window.location.origin}/status/${result.claim_token}`
  const approved = result.status === 'APPROVED'
  return (
    <motion.div initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }} className="glass rounded-2xl p-8 text-center">
      <motion.div
        initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 260, damping: 16, delay: 0.1 }}
        className={cn('mx-auto flex size-16 items-center justify-center rounded-full text-white shadow-lg', approved ? 'bg-success shadow-success/30' : 'bg-warning shadow-warning/30')}
      >
        {approved ? <Check className="size-8" strokeWidth={3} /> : <FileCheck2 className="size-7" />}
      </motion.div>
      <h1 className="mt-6 text-2xl font-semibold tracking-tight">{approved ? 'Claim approved!' : 'Claim submitted'}</h1>
      <p className="mt-2 text-muted-foreground">
        {approved
          ? `You'll receive ${formatAmount(airdrop.claim_amount)} ${airdrop.token_symbol} when the airdrop is launched.`
          : 'The team will review your proof. Check back on your status page.'}
      </p>
      <div className="mt-6 rounded-xl border border-warning/30 bg-warning/10 p-4 text-left">
        <p className="text-sm font-medium">Save your private status link</p>
        <p className="text-xs text-muted-foreground">It's shown only once and is the only way to check your claim.</p>
        <div className="mt-3 flex items-center gap-2 rounded-md border bg-background/60 px-3 py-2">
          <span className="min-w-0 flex-1 truncate font-mono text-xs">{statusUrl}</span>
          <CopyButton value={statusUrl} label="Status link copied" />
        </div>
      </div>
      <Button asChild variant="brand" className="mt-6"><Link to={`/status/${result.claim_token}`}>View status <ArrowRight /></Link></Button>
    </motion.div>
  )
}
