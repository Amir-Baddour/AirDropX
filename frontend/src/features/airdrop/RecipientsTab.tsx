import { useMemo, useRef, useState } from 'react'
import { flexRender, getCoreRowModel, getPaginationRowModel, getSortedRowModel, useReactTable, type ColumnDef, type SortingState } from '@tanstack/react-table'
import { ArrowUpDown, FileUp, Lock, Upload, Users } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input, NativeSelect, Textarea } from '@/components/ui/input'
import { Field } from '@/components/ui/label'
import { EmptyState, Mono, Skeleton } from '@/components/ui/misc'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { RecipientStatusBadge } from '@/components/common/status'
import { ErrorBox, toastError } from '@/components/common/errors'
import { useAddRecipients, useRecipients } from '@/lib/queries'
import { parseRecipients } from '@/lib/recipients'
import type { Airdrop, Recipient } from '@/lib/types'
import { formatAmount, shortAddress, timeAgo } from '@/lib/utils'

export default function RecipientsTab({ airdrop }: { airdrop: Airdrop }) {
  const [status, setStatus] = useState('')
  const list = useRecipients(airdrop.id, status || undefined, airdrop.status === 'PROCESSING')
  const editable = airdrop.status === 'DRAFT'

  return (
    <div className="grid gap-4">
      {editable ? <AddRecipients airdrop={airdrop} /> : (
        <p className="flex items-center gap-2 text-sm text-muted-foreground"><Lock className="size-4" /> The recipient list is frozen after validation.</p>
      )}
      <Card>
        <CardHeader className="flex-row items-center justify-between gap-3">
          <div>
            <CardTitle>Recipients</CardTitle>
            <CardDescription>{airdrop.stats?.total_recipients ?? 0} total{airdrop.status === 'PROCESSING' && ' · updating live'}</CardDescription>
          </div>
          <NativeSelect value={status} onChange={(e) => setStatus(e.target.value)} className="w-40">
            <option value="">All statuses</option>
            {['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'].map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
          </NativeSelect>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {list.isLoading ? <div className="space-y-2 p-5 pt-0">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-9" />)}</div>
            : list.error ? <ErrorBox error={list.error} className="m-5 mt-0" />
            : !list.data?.length ? <EmptyState icon={<Users />} title="No recipients" description={editable ? 'Paste addresses above, or approved claims will appear here.' : undefined} className="m-5 mt-0" />
            : <RecipientTable data={list.data} symbol={airdrop.token_symbol} />}
        </CardContent>
      </Card>
    </div>
  )
}

function RecipientTable({ data, symbol }: { data: Recipient[]; symbol: string }) {
  const [sorting, setSorting] = useState<SortingState>([])
  const columns = useMemo<ColumnDef<Recipient>[]>(
    () => [
      { accessorKey: 'address', header: 'Address', cell: ({ getValue }) => <Mono value={getValue<string>()} display={shortAddress(getValue<string>(), 8, 6)} /> },
      {
        accessorKey: 'amount', header: `Amount (${symbol})`,
        sortingFn: (a, b) => Number(a.original.amount) - Number(b.original.amount),
        cell: ({ getValue }) => <span className="font-mono tabular-nums">{formatAmount(getValue<string>())}</span>,
      },
      { accessorKey: 'status', header: 'Status', cell: ({ row }) => <RecipientStatusBadge status={row.original.status} /> },
      {
        id: 'tx', header: 'Tx / error', enableSorting: false,
        cell: ({ row }) => row.original.tx_ref ? <Mono value={row.original.tx_ref} display={shortAddress(row.original.tx_ref, 10, 6)} />
          : row.original.error ? <span className="text-xs text-destructive">{row.original.error}</span> : <span className="text-muted-foreground">—</span>,
      },
      { accessorKey: 'updated_at', header: 'Updated', cell: ({ getValue }) => <span className="text-muted-foreground">{timeAgo(getValue<string>())}</span> },
    ],
    [symbol],
  )
  // TanStack Table is not React-Compiler-friendly; the component simply is not auto-memoized.
  // eslint-disable-next-line react-hooks/incompatible-library
  const table = useReactTable({
    data, columns, state: { sorting }, onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(), getSortedRowModel: getSortedRowModel(), getPaginationRowModel: getPaginationRowModel(),
    initialState: { pagination: { pageSize: 25 } },
  })
  return (
    <>
      <Table>
        <THead>
          {table.getHeaderGroups().map((hg) => (
            <TR key={hg.id} className="hover:bg-transparent">
              {hg.headers.map((h) => (
                <TH key={h.id}>
                  {h.column.getCanSort() ? (
                    <button className="inline-flex items-center gap-1 uppercase cursor-pointer" onClick={h.column.getToggleSortingHandler()}>
                      {flexRender(h.column.columnDef.header, h.getContext())} <ArrowUpDown className="size-3" />
                    </button>
                  ) : flexRender(h.column.columnDef.header, h.getContext())}
                </TH>
              ))}
            </TR>
          ))}
        </THead>
        <TBody>
          {table.getRowModel().rows.map((r) => (
            <TR key={r.id}>{r.getVisibleCells().map((c) => <TD key={c.id}>{flexRender(c.column.columnDef.cell, c.getContext())}</TD>)}</TR>
          ))}
        </TBody>
      </Table>
      {table.getPageCount() > 1 && (
        <div className="flex items-center justify-end gap-2 border-t p-3 text-sm">
          <span className="text-muted-foreground">Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}</span>
          <Button size="sm" variant="outline" disabled={!table.getCanPreviousPage()} onClick={() => table.previousPage()}>Previous</Button>
          <Button size="sm" variant="outline" disabled={!table.getCanNextPage()} onClick={() => table.nextPage()}>Next</Button>
        </div>
      )}
    </>
  )
}

function AddRecipients({ airdrop }: { airdrop: Airdrop }) {
  const [text, setText] = useState('')
  const [defaultAmount, setDefaultAmount] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)
  const add = useAddRecipients(airdrop.id)
  const parsed = useMemo(() => parseRecipients(text, defaultAmount || undefined), [text, defaultAmount])

  const submit = async () => {
    try {
      const r = await add.mutateAsync(parsed.rows)
      toast.success(`${r.added} recipient(s) added`, { description: r.skipped_existing ? `${r.skipped_existing} were already in the list` : undefined })
      setText('')
    } catch (e) {
      toastError(e, 'Upload failed')
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Add recipients</CardTitle>
        <CardDescription>One per line: <span className="font-mono">address,amount</span>. EVM (0x…) and Solana addresses. Paste or upload a CSV.</CardDescription>
      </CardHeader>
      <CardContent className="grid gap-3">
        <Textarea
          rows={6}
          value={text}
          onChange={(e) => setText(e.target.value)}
          className="font-mono text-xs"
          placeholder={'0x1111111111111111111111111111111111111111,100\n0x2222222222222222222222222222222222222222,250.5'}
        />
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Default amount" hint="Used for lines without an amount" className="w-44">
            <Input value={defaultAmount} onChange={(e) => setDefaultAmount(e.target.value)} placeholder="e.g. 10" inputMode="decimal" />
          </Field>
          <input
            ref={fileRef}
            type="file"
            accept=".csv,.txt,text/csv,text/plain"
            className="hidden"
            onChange={async (e) => {
              const f = e.target.files?.[0]
              if (f) setText(await f.text())
              e.target.value = ''
            }}
          />
          <Button variant="outline" onClick={() => fileRef.current?.click()}><FileUp /> Upload CSV</Button>
          <div className="ml-auto flex items-center gap-3">
            {text.trim() && (
              <span className="text-sm text-muted-foreground">
                <span className="font-medium text-foreground">{parsed.rows.length}</span> valid
                {parsed.errors.length > 0 && <> · <span className="text-destructive">{parsed.errors.length} error(s)</span></>}
              </span>
            )}
            <Button onClick={submit} disabled={!parsed.rows.length || parsed.errors.length > 0} loading={add.isPending}><Upload /> Add {parsed.rows.length || ''}</Button>
          </div>
        </div>
        {parsed.errors.length > 0 && (
          <ul className="max-h-32 list-disc overflow-y-auto rounded-md border border-destructive/30 bg-destructive/10 py-2 pl-7 pr-3 text-xs text-destructive">
            {parsed.errors.slice(0, 50).map((e, i) => <li key={i}>{e}</li>)}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
