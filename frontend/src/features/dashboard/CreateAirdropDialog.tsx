import { useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Input, Textarea } from '@/components/ui/input'
import { Field } from '@/components/ui/label'
import { toastError } from '@/components/common/errors'
import { useCreateAirdrop } from '@/lib/queries'
import { toast } from 'sonner'

const schema = z.object({
  name: z.string().trim().min(1, 'Required').max(150),
  token_symbol: z.string().trim().regex(/^[A-Za-z0-9]{1,15}$/, '1–15 letters or digits'),
  description: z.string().max(2000).optional(),
})
type Values = z.infer<typeof schema>

export default function CreateAirdropDialog({ trigger }: { trigger: ReactNode }) {
  const [open, setOpen] = useState(false)
  const create = useCreateAirdrop()
  const navigate = useNavigate()
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: '', token_symbol: '', description: '' } })
  const e = form.formState.errors

  const submit = async (v: Values) => {
    try {
      const a = await create.mutateAsync({ ...v, token_symbol: v.token_symbol.toUpperCase(), description: v.description || undefined })
      toast.success('Airdrop created')
      setOpen(false)
      form.reset()
      navigate(`/app/airdrops/${a.id}`)
    } catch (err) {
      toastError(err)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New airdrop</DialogTitle>
          <DialogDescription>It starts as a draft. You can add recipients, tasks and open claims next.</DialogDescription>
        </DialogHeader>
        <form className="grid gap-4" onSubmit={form.handleSubmit(submit)}>
          <Field label="Name" htmlFor="ad-name" error={e.name?.message}>
            <Input id="ad-name" placeholder="Mainnet launch drop" autoFocus {...form.register('name')} aria-invalid={!!e.name} />
          </Field>
          <Field label="Token symbol" htmlFor="ad-sym" error={e.token_symbol?.message}>
            <Input id="ad-sym" placeholder="ACME" className="font-mono uppercase" {...form.register('token_symbol')} aria-invalid={!!e.token_symbol} />
          </Field>
          <Field label="Description" htmlFor="ad-desc" hint="Shown on the public claim page.">
            <Textarea id="ad-desc" placeholder="Complete the tasks to earn tokens." {...form.register('description')} />
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" loading={create.isPending}>Create airdrop</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
