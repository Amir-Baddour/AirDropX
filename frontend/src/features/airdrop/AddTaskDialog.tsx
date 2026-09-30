import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input, Textarea } from '@/components/ui/input'
import { Field, Label } from '@/components/ui/label'
import { toastError } from '@/components/common/errors'
import { useCreateTask } from '@/lib/queries'
import type { QuizQuestion, TaskType } from '@/lib/types'
import { cn } from '@/lib/utils'
import { TASK_META } from './TasksTab'

const emptyQuestion = (): QuizQuestion => ({ question: '', options: ['', ''], answer: 0 })

export default function AddTaskDialog({ airdropId, open, onOpenChange }: { airdropId: string; open: boolean; onOpenChange: (o: boolean) => void }) {
  const create = useCreateTask(airdropId)
  const [type, setType] = useState<TaskType>('QUIZ')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [questions, setQuestions] = useState<QuizQuestion[]>([emptyQuestion()])
  const [code, setCode] = useState('')
  const [hint, setHint] = useState('')
  const [instructions, setInstructions] = useState('')
  const [error, setError] = useState<string | null>(null)

  const reset = () => {
    setTitle(''); setDescription(''); setQuestions([emptyQuestion()]); setCode(''); setHint(''); setInstructions(''); setError(null)
  }

  const validate = (): string | null => {
    if (!title.trim()) return 'Give the task a title'
    if (type === 'QUIZ') {
      for (const [i, q] of questions.entries()) {
        if (!q.question.trim()) return `Question ${i + 1} needs text`
        if (q.options.some((o) => !o.trim())) return `Question ${i + 1} has an empty option`
      }
    }
    if (type === 'SECRET_CODE' && (code.trim().length < 4 || code.trim().length > 64)) return 'The code must be 4–64 characters'
    if (type === 'MANUAL_PROOF' && !instructions.trim()) return 'Tell claimants what proof to send'
    return null
  }

  const submit = async () => {
    const problem = validate()
    setError(problem)
    if (problem) return
    const config =
      type === 'QUIZ' ? { questions: questions.map((q) => ({ question: q.question.trim(), options: q.options.map((o) => o.trim()), answer: q.answer })) }
        : type === 'SECRET_CODE' ? { code: code.trim(), ...(hint.trim() ? { hint: hint.trim() } : {}) }
          : { instructions: instructions.trim() }
    try {
      await create.mutateAsync({ type, title: title.trim(), description: description.trim() || undefined, config })
      toast.success('Task added')
      reset()
      onOpenChange(false)
    } catch (e) {
      toastError(e)
    }
  }

  const updateQ = (i: number, patch: Partial<QuizQuestion>) => setQuestions((qs) => qs.map((q, j) => (j === i ? { ...q, ...patch } : q)))

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Add a task</DialogTitle>
          <DialogDescription>Choose how claimants prove they did something.</DialogDescription>
        </DialogHeader>

        <div className="grid gap-2 sm:grid-cols-3">
          {(Object.keys(TASK_META) as TaskType[]).map((t) => {
            const { label, icon: Icon, blurb } = TASK_META[t]
            return (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                className={cn(
                  'rounded-lg border p-3 text-left transition-colors cursor-pointer hover:bg-accent',
                  type === t && 'border-primary bg-primary/5 ring-1 ring-primary',
                )}
              >
                <Icon className="size-5 text-primary" />
                <p className="mt-2 text-sm font-medium">{label}</p>
                <p className="text-xs text-muted-foreground">{blurb}</p>
              </button>
            )
          })}
        </div>

        <div className="grid gap-4">
          <Field label="Title" htmlFor="t-title">
            <Input id="t-title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder={type === 'QUIZ' ? 'Read our docs' : type === 'SECRET_CODE' ? 'Watch the launch video' : 'Post about us on X'} maxLength={150} />
          </Field>
          <Field label="Description (optional)" htmlFor="t-desc">
            <Input id="t-desc" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Link or extra context for claimants" maxLength={1000} />
          </Field>

          {type === 'QUIZ' && (
            <div className="grid gap-3">
              {questions.map((q, i) => (
                <div key={i} className="rounded-lg border p-3">
                  <div className="flex items-center gap-2">
                    <Label className="shrink-0 text-xs text-muted-foreground">Q{i + 1}</Label>
                    <Input value={q.question} onChange={(e) => updateQ(i, { question: e.target.value })} placeholder="Question" maxLength={300} />
                    {questions.length > 1 && (
                      <Button type="button" variant="ghost" size="icon" onClick={() => setQuestions((qs) => qs.filter((_, j) => j !== i))} aria-label="Remove question"><Trash2 /></Button>
                    )}
                  </div>
                  <div className="mt-2 grid gap-1.5 pl-7">
                    {q.options.map((o, k) => (
                      <div key={k} className="flex items-center gap-2">
                        <input
                          type="radio"
                          name={`answer-${i}`}
                          checked={q.answer === k}
                          onChange={() => updateQ(i, { answer: k })}
                          className="size-4 accent-[var(--success)] cursor-pointer"
                          aria-label={`Mark option ${k + 1} as correct`}
                        />
                        <Input
                          value={o}
                          onChange={(e) => updateQ(i, { options: q.options.map((x, j) => (j === k ? e.target.value : x)) })}
                          placeholder={`Option ${k + 1}`}
                          className={cn('h-8', q.answer === k && 'border-success/50')}
                          maxLength={200}
                        />
                        {q.options.length > 2 && (
                          <Button
                            type="button" variant="ghost" size="icon" className="size-8"
                            onClick={() => updateQ(i, { options: q.options.filter((_, j) => j !== k), answer: q.answer === k ? 0 : (q.answer ?? 0) > k ? (q.answer ?? 0) - 1 : q.answer })}
                            aria-label="Remove option"
                          ><Trash2 /></Button>
                        )}
                      </div>
                    ))}
                    {q.options.length < 6 && (
                      <button type="button" className="w-fit text-xs text-primary hover:underline cursor-pointer" onClick={() => updateQ(i, { options: [...q.options, ''] })}>+ Add option</button>
                    )}
                  </div>
                </div>
              ))}
              <p className="text-xs text-muted-foreground">Select the radio button next to the correct answer.</p>
              {questions.length < 10 && (
                <Button type="button" variant="outline" size="sm" className="w-fit" onClick={() => setQuestions((qs) => [...qs, emptyQuestion()])}><Plus /> Add question</Button>
              )}
            </div>
          )}

          {type === 'SECRET_CODE' && (
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Secret code" htmlFor="t-code" hint="Case and spaces are ignored. Stored only as a hash.">
                <Input id="t-code" value={code} onChange={(e) => setCode(e.target.value)} placeholder="MOON2026" className="font-mono" maxLength={64} />
              </Field>
              <Field label="Hint (optional)" htmlFor="t-hint" hint="Shown to claimants">
                <Input id="t-hint" value={hint} onChange={(e) => setHint(e.target.value)} placeholder="Shown at the end of the video" maxLength={300} />
              </Field>
            </div>
          )}

          {type === 'MANUAL_PROOF' && (
            <Field label="Instructions" htmlFor="t-ins" hint="What should claimants send? You'll review each one.">
              <Textarea id="t-ins" value={instructions} onChange={(e) => setInstructions(e.target.value)} placeholder="Paste the link to your post about us" maxLength={500} />
            </Field>
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={submit} loading={create.isPending}>Add task</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
