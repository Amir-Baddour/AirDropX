import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Linkify } from './linkify'

describe('Linkify', () => {
  it('turns URLs into safe links and keeps the text around them', () => {
    const { container } = render(<Linkify text="Comment on https://www.instagram.com/p/Dc8f9bAm9L8/. Thanks!" />)
    const a = container.querySelector('a')!
    expect(a.getAttribute('href')).toBe('https://www.instagram.com/p/Dc8f9bAm9L8/')
    expect(a.getAttribute('rel')).toContain('noopener')
    expect(a.getAttribute('target')).toBe('_blank')
    expect(container.textContent).toBe('Comment on https://www.instagram.com/p/Dc8f9bAm9L8/. Thanks!')
  })
  it('does not link javascript: or plain text', () => {
    const { container } = render(<Linkify text="javascript:alert(1) and www.example.com" />)
    expect(container.querySelector('a')).toBeNull()
  })
})
