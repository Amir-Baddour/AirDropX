import { isValidAddress } from './utils'

export interface ParsedRecipients {
  rows: { address: string; amount: string }[]
  errors: string[]
}

const AMOUNT = /^\d+(\.\d{1,18})?$/

/**
 * Parses "address,amount" lines (comma, semicolon, tab or spaces). A header line is skipped.
 * Mirrors the backend rules so most mistakes are caught before sending.
 */
export function parseRecipients(text: string, defaultAmount?: string): ParsedRecipients {
  const rows: { address: string; amount: string }[] = []
  const errors: string[] = []
  const seen = new Set<string>()
  const lines = text.split(/\r?\n/)
  lines.forEach((raw, i) => {
    const line = raw.trim()
    if (!line || line.startsWith('#')) return
    const [address = '', amountRaw = ''] = line.split(/[,;\t ]+/)
    if (i === 0 && !isValidAddress(address) && /address|wallet/i.test(address)) return // header
    const amount = amountRaw || defaultAmount || ''
    const n = i + 1
    if (!isValidAddress(address)) return void errors.push(`Line ${n}: '${address.slice(0, 20)}' is not a valid EVM or Solana address`)
    if (!AMOUNT.test(amount) || Number(amount) <= 0) return void errors.push(`Line ${n}: amount '${amount || '(missing)'}' must be a positive number (max 18 decimals)`)
    const key = address.startsWith('0x') ? address.toLowerCase() : address
    if (seen.has(key)) return void errors.push(`Line ${n}: duplicate address ${address.slice(0, 10)}…`)
    seen.add(key)
    rows.push({ address, amount })
  })
  if (rows.length > 10000) errors.push('Maximum 10,000 recipients per upload')
  return { rows, errors }
}
