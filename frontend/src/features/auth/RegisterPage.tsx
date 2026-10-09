import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, Navigate, useNavigate } from 'react-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Field } from '@/components/ui/label'
import { GlowBackground } from '@/components/common/glow-background'
import { Logo } from '@/components/common/logo'
import { toastError } from '@/components/common/errors'
import { api, ApiError } from '@/lib/api'
import { useSession } from '@/lib/auth'
import { passwordError } from '@/lib/password'

const schema = z
  .object({
    firstName: z.string().trim().min(1, 'Required').max(100),
    lastName: z.string().trim().min(1, 'Required').max(100),
    email: z.string().trim().email('Enter a valid email address').max(254),
    password: z.string().superRefine((v, ctx) => {
      const message = passwordError(v)
      if (message) ctx.addIssue({ code: z.ZodIssueCode.custom, message })
    }),
    confirm: z.string(),
  })
  .refine((v) => v.password === v.confirm, { path: ['confirm'], message: 'Passwords do not match' })

type Values = z.infer<typeof schema>

export default function RegisterPage() {
  const { isAuthenticated } = useSession()
  const navigate = useNavigate()
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { firstName: '', lastName: '', email: '', password: '', confirm: '' },
  })
  const errors = form.formState.errors

  if (isAuthenticated) return <Navigate to="/app" replace />

  const onSubmit = async (v: Values) => {
    try {
      await api.post('/auth/register', { email: v.email, password: v.password, firstName: v.firstName, lastName: v.lastName }, false)
      // Registering does not sign you in: go to the login page with the email filled in.
      navigate(`/login?registered=1&email=${encodeURIComponent(v.email)}`, { replace: true })
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) form.setError('email', { message: e.message })
      else toastError(e, 'Could not create the account')
    }
  }

  return (
    <div className="relative flex min-h-dvh items-center justify-center px-4 py-10">
      <GlowBackground />
      <div className="glass w-full max-w-sm rounded-2xl p-8">
        <Logo className="mb-8 text-lg" />
        <h1 className="text-2xl font-semibold tracking-tight">Create your account</h1>
        <p className="mt-1 text-sm text-muted-foreground">Then you will sign in and set up your company.</p>

        <form className="mt-6 grid gap-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <div className="grid grid-cols-2 gap-3">
            <Field label="First name" htmlFor="firstName" error={errors.firstName?.message}>
              <Input id="firstName" autoComplete="given-name" autoFocus {...form.register('firstName')} aria-invalid={!!errors.firstName} />
            </Field>
            <Field label="Last name" htmlFor="lastName" error={errors.lastName?.message}>
              <Input id="lastName" autoComplete="family-name" {...form.register('lastName')} aria-invalid={!!errors.lastName} />
            </Field>
          </div>
          <Field label="Email" htmlFor="email" error={errors.email?.message}>
            <Input id="email" type="email" autoComplete="email" {...form.register('email')} aria-invalid={!!errors.email} />
          </Field>
          <Field label="Password" htmlFor="password" error={errors.password?.message} hint="At least 8 characters, with a letter and a number">
            <Input id="password" type="password" autoComplete="new-password" {...form.register('password')} aria-invalid={!!errors.password} />
          </Field>
          <Field label="Confirm password" htmlFor="confirm" error={errors.confirm?.message}>
            <Input id="confirm" type="password" autoComplete="new-password" {...form.register('confirm')} aria-invalid={!!errors.confirm} />
          </Field>
          <Button type="submit" loading={form.formState.isSubmitting}>Create account</Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          Already have an account? <Link to="/login" className="text-foreground underline-offset-4 hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
