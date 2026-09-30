import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** 0x1234…abcd */
export function shortAddress(address: string | null | undefined, head = 6, tail = 4) {
  if (!address) return ''
  return address.length <= head + tail + 1 ? address : `${address.slice(0, head)}…${address.slice(-tail)}`
}

/** Formats a decimal string without losing precision (amounts can have 18 decimals). */
export function formatAmount(value: string | null | undefined, maxDecimals = 4) {
  if (value == null || value === '') return '—'
  const [int, frac = ''] = value.split('.')
  const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const trimmed = frac.slice(0, maxDecimals).replace(/0+$/, '')
  return trimmed ? `${grouped}.${trimmed}` : grouped
}

export function formatDate(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function timeAgo(iso: string | null | undefined) {
  if (!iso) return ''
  const s = Math.round((Date.now() - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'just now'
  const m = Math.round(s / 60)
  if (m < 60) return `${m}m ago`
  const h = Math.round(m / 60)
  if (h < 48) return `${h}h ago`
  return `${Math.round(h / 24)}d ago`
}

export const EVM_ADDRESS = /^0x[a-fA-F0-9]{40}$/
export const SOLANA_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/
export const isValidAddress = (a: string) => EVM_ADDRESS.test(a.trim()) || SOLANA_ADDRESS.test(a.trim())
