import { describe, expect, it } from 'vitest'
import { passwordError } from './password'

describe('passwordError', () => {
  it('accepts a password with letters and numbers', () => {
    expect(passwordError('abc12345')).toBeNull()
  })
  it('rejects short, letter-only and number-only passwords', () => {
    expect(passwordError('abc1')).toMatch(/at least 8/i)
    expect(passwordError('onlyletters')).toMatch(/letter and one number/i)
    expect(passwordError('12345678')).toMatch(/letter and one number/i)
  })
  it('rejects very long passwords', () => {
    expect(passwordError('a1'.repeat(70))).toMatch(/at most/i)
  })
})
