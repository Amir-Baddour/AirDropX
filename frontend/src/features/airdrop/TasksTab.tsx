import { useState } from 'react'
import { BrainCircuit, CheckCircle2, FileCheck2, KeyRound, ListChecks, Lock, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Confirm } from '@/components/ui/confirm'
import { EmptyState, Skeleton } from '@/components/ui/misc'
import { ErrorBox, toastError } from '@/components/common/errors'
import { useDeleteTask, useTasks } from '@/lib/queries'
import type { Airdrop, Task, TaskType } from '@/lib/types'
import AddTaskDialog from './AddTaskDialog'
import { Linkify } from '@/components/common/linkify'

export const TASK_META: Record<TaskType, { label: string; icon: typeof KeyRound; blurb: string }> = {
  QUIZ: { label: 'Quiz', icon: BrainCircuit, blurb: 'Multiple-choice questions, checked instantly' },
  SECRET_CODE: { label: 'Secret code', icon: KeyRound, blurb: 'A code hidden in your video, blog or stream' },
  MANUAL_PROOF: { label: 'Manual proof', icon: FileCheck2, blurb: 'A link or text you review by hand' },
}

export default function TasksTab({ airdrop }: { airdrop: Airdrop }) {
  const tasks = useTasks(airdrop.id)
  const del = useDeleteTask(airdrop.id)
  const [adding, setAdding] = useState(false)
  const locked = airdrop.status !== 'DRAFT' || airdrop.claims_open
  const lockReason = airdrop.status !== 'DRAFT' ? 'Tasks can only change while the airdrop is a draft.' : 'Close claims to change tasks.'

  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-muted-foreground">
          Claimants must complete every task. Auto-verified tasks are checked instantly; manual ones go to your review queue.
        </p>
        {locked ? (
          <span className="flex items-center gap-1.5 text-sm text-muted-foreground"><Lock className="size-4" /> {lockReason}</span>
        ) : (
          <Button onClick={() => setAdding(true)} disabled={(tasks.data?.length ?? 0) >= 10}><Plus /> Add task</Button>
        )}
      </div>

      {tasks.isLoading ? <Skeleton className="h-24" /> : tasks.error ? <ErrorBox error={tasks.error} /> : !tasks.data?.length ? (
        <EmptyState
          icon={<ListChecks />}
          title="No tasks"
          description="Without tasks, anyone with a valid wallet is approved instantly. Add a quiz or secret code to reach real people."
          action={!locked && <Button onClick={() => setAdding(true)}><Plus /> Add task</Button>}
        />
      ) : (
        <div className="grid gap-3">
          {tasks.data.map((t, i) => (
            <TaskCard key={t.id} task={t} index={i} locked={locked} onDelete={() => del.mutateAsync(t.id).then(() => toast.success('Task deleted')).catch((e) => { toastError(e); throw e })} />
          ))}
        </div>
      )}

      <AddTaskDialog airdropId={airdrop.id} open={adding} onOpenChange={setAdding} />
    </div>
  )
}

function TaskCard({ task, index, locked, onDelete }: { task: Task; index: number; locked: boolean; onDelete: () => Promise<unknown> }) {
  const meta = TASK_META[task.type]
  const Icon = meta.icon
  return (
    <Card className="flex gap-4 p-4">
      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary"><Icon className="size-5" /></div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs text-muted-foreground">#{index + 1}</span>
          <p className="font-medium">{task.title}</p>
          <Badge tone="neutral">{meta.label}</Badge>
          {task.auto_verified ? <Badge tone="success"><CheckCircle2 className="size-3" /> Auto-verified</Badge> : <Badge tone="warning">Manual review</Badge>}
        </div>
        {task.description && <p className="mt-1 text-sm text-muted-foreground"><Linkify text={task.description} /></p>}
        <div className="mt-2 text-sm">
          {task.type === 'QUIZ' && (
            <ol className="list-decimal space-y-1 pl-5 text-muted-foreground">
              {task.config.questions?.map((q, i) => (
                <li key={i}>{q.question} <span className="text-success">→ {q.options[q.answer ?? 0]}</span></li>
              ))}
            </ol>
          )}
          {task.type === 'SECRET_CODE' && (
            <p className="text-muted-foreground">Code stored as a hash only{task.config.hint && <> · hint: “{task.config.hint}”</>}</p>
          )}
          {task.type === 'MANUAL_PROOF' && <p className="text-muted-foreground"><Linkify text={task.config.instructions ?? ''} /></p>}
        </div>
      </div>
      {!locked && (
        <Confirm
          trigger={<Button variant="ghost" size="icon" aria-label="Delete task"><Trash2 /></Button>}
          title="Delete this task?"
          description="Results already stored for this task are removed too."
          confirmLabel="Delete"
          destructive
          onConfirm={onDelete}
        />
      )}
    </Card>
  )
}
