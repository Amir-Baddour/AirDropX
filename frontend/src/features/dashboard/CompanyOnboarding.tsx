import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link } from 'react-router'
import { Building2, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Field } from '@/components/ui/label'
import { toastError } from '@/components/common/errors'
import { useCreateCompany } from '@/lib/queries'

const schema = z.object({ name: z.string().trim().min(2, 'At least 2 characters').max(150) })

export default function CompanyOnboarding({ isAdmin = false }: { isAdmin?: boolean }) {
  const create = useCreateCompany()
  const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema), defaultValues: { name: '' } })
  return (
    <div className="mx-auto max-w-md pt-10">
      <div className="mb-6 flex size-12 items-center justify-center rounded-xl brand-gradient text-white"><Building2 /></div>
      <h1 className="text-2xl font-semibold tracking-tight">Create your company</h1>
      <p className="mt-1 text-sm text-muted-foreground">Airdrops belong to a company. You'll be its owner.</p>
      <Card className="mt-6">
        <CardContent className="pt-5">
          <form className="grid gap-4" onSubmit={form.handleSubmit((v) => create.mutateAsync(v.name).catch(toastError))}>
            <Field label="Company name" htmlFor="name" error={form.formState.errors.name?.message}>
              <Input id="name" autoFocus placeholder="Acme Labs" {...form.register('name')} aria-invalid={!!form.formState.errors.name} />
            </Field>
            <Button type="submit" loading={create.isPending}>Create company</Button>
          </form>
        </CardContent>
      </Card>
      {isAdmin && (
        <Button variant="link" asChild className="mt-4 px-0"><Link to="/admin"><ShieldCheck /> Go to platform admin instead</Link></Button>
      )}
    </div>
  )
}
