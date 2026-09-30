import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from './api'
import { getToken, saveSession } from './session'

const respond = (status: number, body: string) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(body, { status }))

/** Resolves with the ApiError a request rejects with. */
const fail = (p: Promise<unknown>) =>
  p.then(
    () => { throw new Error('expected the request to fail') },
    (e) => e as ApiError,
  )

afterEach(() => {
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('api client', () => {
  it('unwraps { data } and sends the bearer token to /api', async () => {
    saveSession('tok123', { id: 'u', username: 'u', pfp: null })
    const spy = respond(200, JSON.stringify({ success: true, message: 'ok', data: { id: 1 } }))
    await expect(api.get('/airdrops')).resolves.toEqual({ id: 1 })
    const [url, init] = spy.mock.calls[0]
    expect(url).toBe('/api/airdrops')
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer tok123')
  })

  it('keeps the 422 error list', async () => {
    respond(422, JSON.stringify({ success: false, message: 'Some tasks are not completed', errors: ['Quiz: wrong'] }))
    const err = await fail(api.post('/public/airdrops/x/claims', {}, false))
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(422)
    expect(err.errors).toEqual(['Quiz: wrong'])
  })

  it('treats the plain-text auth codes as an expired session and signs out', async () => {
    saveSession('old', { id: 'u', username: 'u', pfp: null })
    respond(403, 'EXPIRED_TOKEN')
    const err = await fail(api.get('/companies/me'))
    expect(err.isAuth).toBe(true)
    expect(getToken()).toBeNull()
  })

  it('a JSON 403 (no company) is not treated as a sign-out', async () => {
    saveSession('tok', { id: 'u', username: 'u', pfp: null })
    respond(403, JSON.stringify({ success: false, message: 'You do not belong to a company' }))
    const err = await fail(api.get('/airdrops'))
    expect(err.isAuth).toBe(false)
    expect(getToken()).toBe('tok')
  })
})
