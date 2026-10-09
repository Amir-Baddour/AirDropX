import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Field } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/misc'
import { ErrorBox } from '@/components/common/errors'
import { toastError } from '@/components/common/errors'
import { PageHeader } from '@/components/layout/PageHeader'
import { ApiError } from '@/lib/api'
import { passwordError } from '@/lib/password'
import { useChangePassword, useProfile, useUpdateProfile } from '@/lib/queries'
import { updateSessionUser } from '@/lib/session'
import { formatDate } from '@/lib/utils'

const profileSchema = z.object({
  firstName: z.string().trim().min(1, 'Required').max(100),
  lastName: z.string().trim().min(1, 'Required').max(100),
  phone: z.string().trim().max(25).regex(/^(\+?[0-9 ()-]{6,25})?$/, 'Enter a valid phone number'),
  address: z.string().trim().max(255),
})
type ProfileValues = z.infer<typeof profileSchema>

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Required'),
    newPassword: z.string().superRefine((v, ctx) => {
      const message = passwordError(v)
      if (message) ctx.addIssue({ code: z.ZodIssueCode.custom, message })
    }),
    confirm: z.string(),
  })
  .refine((v) => v.newPassword === v.confirm, { path: ['confirm'], message: 'Passwords do not match' })
type PasswordValues = z.infer<typeof passwordSchema>

export default function ProfilePage() {
  const { data: profile, isLoading, error } = useProfile()

  if (isLoading) return <div className="max-w-2xl space-y-3"><Skeleton className="h-8 w-48" /><Skeleton className="h-64 w-full" /></div>
  if (error || !profile) return <ErrorBox error={error} />

  return (
    <>
      <PageHeader title="Profile" description="Your personal details and how you sign in." />
      <div className="grid max-w-2xl gap-6">
        <DetailsCard />
        {profile.has_password ? (
          <PasswordCard />
        ) : (
          <Card>
            <CardHeader>
              <CardTitle>Password</CardTitle>
              <CardDescription>This account signs in with Google, so there is no password to manage here.</CardDescription>
            </CardHeader>
          </Card>
        )}
      </div>
    </>
  )
}

function DetailsCard() {
  const { data: profile } = useProfile()
  const update = useUpdateProfile()
  const form = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { firstName: '', lastName: '', phone: '', address: '' },
  })
  const errors = form.formState.errors

  useEffect(() => {
    if (!profile) return
    // Google accounts start without a first/last name: suggest one from the Google display name.
    const [guessFirst = '', ...guessRest] = profile.provider === 'GOOGLE' ? profile.username.trim().split(/\s+/) : []
    form.reset({
      firstName: profile.first_name ?? guessFirst,
      lastName: profile.last_name ?? guessRest.join(' '),
      phone: profile.phone ?? '',
      address: profile.address ?? '',
    })
  }, [profile, form])

  if (!profile) return null

  const onSubmit = (v: ProfileValues) =>
    update
      .mutateAsync(v)
      .then((saved) => {
        updateSessionUser({ displayName: saved.display_name })
        toast.success('Profile saved')
      })
      .catch((e) => toastError(e, 'Could not save your profile'))

  return (
    <Card>
      <CardHeader>
        <CardTitle>Details</CardTitle>
        <CardDescription>
          Signed in with {profile.provider === 'GOOGLE' ? 'Google' : 'email and password'}
          {profile.created_at ? ` · member since ${formatDate(profile.created_at)}` : ''}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="grid gap-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <Field label="Email" htmlFor="email" hint="Your sign-in email cannot be changed here.">
            <Input id="email" value={profile.email ?? '—'} readOnly disabled />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="First name" htmlFor="firstName" error={errors.firstName?.message}>
              <Input id="firstName" autoComplete="given-name" {...form.register('firstName')} aria-invalid={!!errors.firstName} />
            </Field>
            <Field label="Last name" htmlFor="lastName" error={errors.lastName?.message}>
              <Input id="lastName" autoComplete="family-name" {...form.register('lastName')} aria-invalid={!!errors.lastName} />
            </Field>
          </div>
          <Field label="Phone" htmlFor="phone" error={errors.phone?.message} hint="Optional, for example +961 70 123 456">
            <Input id="phone" type="tel" autoComplete="tel" {...form.register('phone')} aria-invalid={!!errors.phone} />
          </Field>
          <Field label="Address" htmlFor="address" error={errors.address?.message} hint="Optional">
            <Input id="address" autoComplete="street-address" {...form.register('address')} aria-invalid={!!errors.address} />
          </Field>
          <div>
            <Button type="submit" loading={update.isPending} disabled={!form.formState.isDirty && !!profile.first_name}>Save changes</Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

function PasswordCard() {
  const change = useChangePassword()
  const form = useForm<PasswordValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirm: '' },
  })
  const errors = form.formState.errors

  const onSubmit = (v: PasswordValues) =>
    change
      .mutateAsync({ currentPassword: v.currentPassword, newPassword: v.newPassword })
      .then(() => {
        toast.success('Password changed')
        form.reset()
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 403) form.setError('currentPassword', { message: e.message })
        else toastError(e, 'Could not change your password')
      })

  return (
    <Card>
      <CardHeader>
        <CardTitle>Change password</CardTitle>
        <CardDescription>You stay signed in on this device.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="grid gap-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <Field label="Current password" htmlFor="currentPassword" error={errors.currentPassword?.message}>
            <Input id="currentPassword" type="password" autoComplete="current-password" {...form.register('currentPassword')} aria-invalid={!!errors.currentPassword} />
          </Field>
          <Field label="New password" htmlFor="newPassword" error={errors.newPassword?.message} hint="At least 8 characters, with a letter and a number">
            <Input id="newPassword" type="password" autoComplete="new-password" {...form.register('newPassword')} aria-invalid={!!errors.newPassword} />
          </Field>
          <Field label="Confirm new password" htmlFor="confirm" error={errors.confirm?.message}>
            <Input id="confirm" type="password" autoComplete="new-password" {...form.register('confirm')} aria-invalid={!!errors.confirm} />
          </Field>
          <div>
            <Button type="submit" loading={change.isPending}>Change password</Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
