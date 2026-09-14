import { afterEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'

import { callOperation } from './client'

const gameMode = z.object({ name: z.string(), display_name: z.string() })

function serverAnswers(status: number, body: unknown): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () =>
      new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('callOperation', () => {
  it('hands back the data an operation produced', async () => {
    serverAnswers(200, { ok: true, data: { name: 'Pathfinder_RPG', display_name: 'Pathfinder' }, error: null })

    const answer = await callOperation('list_game_modes', {}, gameMode)

    expect(answer).toEqual({ ok: true, data: { name: 'Pathfinder_RPG', display_name: 'Pathfinder' } })
  })

  it('posts the operation name and its arguments', async () => {
    serverAnswers(200, { ok: true, data: { name: 'a', display_name: 'b' }, error: null })

    await callOperation('load_sources', { game_mode: 'Pathfinder_RPG' }, gameMode)

    expect(fetch).toHaveBeenCalledWith(
      '/api/load_sources',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ game_mode: 'Pathfinder_RPG' }),
      }),
    )
  })

  it('hands back the refusal the server gave, with its kind', async () => {
    serverAnswers(404, {
      ok: false,
      data: null,
      error: { kind: 'CharacterNotFound', message: 'Character not found: 42' },
    })

    const answer = await callOperation('get_character', { character_id: '42' }, gameMode)

    expect(answer).toEqual({
      ok: false,
      error: { kind: 'CharacterNotFound', message: 'Character not found: 42' },
    })
  })

  it('refuses data that is not shaped as the caller expects', async () => {
    serverAnswers(200, { ok: true, data: { name: 'Pathfinder_RPG' }, error: null })

    const answer = await callOperation('list_game_modes', {}, gameMode)

    expect(answer.ok).toBe(false)
    expect(answer.ok === false && answer.error.kind).toBe('UnreadableResponse')
  })

  it('reports a body that is not the envelope at all', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('<html>oops</html>', { status: 500 })))

    const answer = await callOperation('list_game_modes', {}, gameMode)

    expect(answer.ok).toBe(false)
    expect(answer.ok === false && answer.error.kind).toBe('UnreadableResponse')
  })

  it('reports a server that cannot be reached', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('Failed to fetch')
      }),
    )

    const answer = await callOperation('list_game_modes', {}, gameMode)

    expect(answer.ok).toBe(false)
    expect(answer.ok === false && answer.error.kind).toBe('ServerUnreachable')
  })
})
