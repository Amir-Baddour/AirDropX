import * as AlertDialog from '@radix-ui/react-alert-dialog'
import * as React from 'react'
import { Button } from './button'

/** Confirmation for irreversible actions (launch, cancel, reject). */
export function Confirm({ trigger, title, description, confirmLabel, destructive, onConfirm, children }: {
  trigger: React.ReactNode
  title: string
  description: React.ReactNode
  confirmLabel: string
  destructive?: boolean
  onConfirm: () => void | Promise<unknown>
  children?: React.ReactNode
}) {
  const [open, setOpen] = React.useState(false)
  const [busy, setBusy] = React.useState(false)
  return (
    <AlertDialog.Root open={open} onOpenChange={setOpen}>
      <AlertDialog.Trigger asChild>{trigger}</AlertDialog.Trigger>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm" />
        <AlertDialog.Content className="fixed left-1/2 top-1/2 z-50 grid w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 gap-4 rounded-xl border bg-card p-6 shadow-2xl">
          <AlertDialog.Title className="text-lg font-semibold">{title}</AlertDialog.Title>
          <AlertDialog.Description className="text-sm text-muted-foreground">{description}</AlertDialog.Description>
          {children}
          <div className="flex justify-end gap-2">
            <AlertDialog.Cancel asChild><Button variant="outline">Cancel</Button></AlertDialog.Cancel>
            <Button
              variant={destructive ? 'destructive' : 'default'}
              loading={busy}
              onClick={async () => {
                setBusy(true)
                try {
                  await onConfirm()
                  setOpen(false)
                } catch {
                  /* the caller shows the error toast; keep the dialog open */
                } finally {
                  setBusy(false)
                }
              }}
            >
              {confirmLabel}
            </Button>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  )
}
