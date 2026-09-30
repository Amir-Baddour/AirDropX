import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Mono } from '@/components/ui/misc'
import { PageHeader } from '@/components/layout/PageHeader'
import { useSession } from '@/lib/auth'
import { useCompany } from '@/lib/queries'
import { formatDate } from '@/lib/utils'

export default function CompanyPage() {
  const { data } = useCompany()
  const { user } = useSession()
  if (!data) return null
  const rows: [string, React.ReactNode][] = [
    ['Name', data.name],
    ['Company ID', <Mono key="id" value={data.id} />],
    ['Created', formatDate(data.created_at)],
    ['Signed in as', user?.username ?? '—'],
  ]
  return (
    <>
      <PageHeader title="Company" description="Every airdrop, task and claim is scoped to this company." />
      <Card className="max-w-2xl">
        <CardHeader>
          <CardTitle>Details</CardTitle>
          <CardDescription>Other companies can never see or change your data.</CardDescription>
        </CardHeader>
        <CardContent>
          <dl className="divide-y">
            {rows.map(([k, v]) => (
              <div key={k} className="flex items-center justify-between gap-4 py-3 text-sm">
                <dt className="text-muted-foreground">{k}</dt>
                <dd className="min-w-0 truncate text-right font-medium">{v}</dd>
              </div>
            ))}
          </dl>
        </CardContent>
      </Card>
    </>
  )
}
