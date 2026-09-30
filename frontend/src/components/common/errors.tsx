import { toast } from 'sonner'
import { ApiError } from '@/lib/api'

/** Shows an API error as a toast, including the per-row list sent with 422. */
export function toastError(err: unknown, fallback = 'Something went wrong') {
  if (err instanceof ApiError) {
    const list = err.errors.slice(0, 5)
    toast.error(err.message || fallback, {
      description: list.length ? list.join('\n') + (err.errors.length > 5 ? `\n…and ${err.errors.length - 5} more` : '') : undefined,
    })
  } else {
    toast.error(fallback)
  }
}

export function ErrorBox({ error, className }: { error: unknown; className?: string }) {
  const message = error instanceof ApiError ? error.message : 'Something went wrong'
  const list = error instanceof ApiError ? error.errors : []
  return (
    <div className={`rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm ${className ?? ''}`}>
      <p className="font-medium text-destructive">{message}</p>
      {list.length > 0 && (
        <ul className="mt-2 max-h-40 list-disc space-y-0.5 overflow-y-auto pl-5 text-destructive/90">
          {list.map((e, i) => <li key={i}>{e}</li>)}
        </ul>
      )}
    </div>
  )
}
