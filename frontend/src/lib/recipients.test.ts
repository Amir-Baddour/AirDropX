import { describe, expect, it } from 'vitest'
import { parseRecipients } from './recipients'

const A = '0x' + '1'.repeat(40)
const B = '0x' + 'a'.repeat(40)

describe('parseRecipients', () => {
  it('parses comma, tab and space separated rows and skips a header', () => {
    const r = parseRecipients(`address,amount\n${A},10\n${B}\t2.5`)
    expect(r.errors).toEqual([])
    expect(r.rows).toEqual([{ address: A, amount: '10' }, { address: B, amount: '2.5' }])
  })
  it('uses the default amount when a row has none', () => {
    expect(parseRecipients(A, '5').rows[0].amount).toBe('5')
  })
  it('reports bad addresses, bad amounts and case-insensitive EVM duplicates', () => {
    const r = parseRecipients(`nope,1\n${A},-3\n${A},1\n${A.toUpperCase().replace('0X', '0x')},1`)
    expect(r.rows).toHaveLength(1)
    expect(r.errors).toHaveLength(3)
    expect(r.errors[0]).toContain('Line 1')
  })
  it('rejects more than 18 decimals', () => {
    expect(parseRecipients(`${A},1.${'1'.repeat(19)}`).errors).toHaveLength(1)
  })
})
