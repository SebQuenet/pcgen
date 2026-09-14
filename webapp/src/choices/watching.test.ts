import { describe, expect, it, vi } from 'vitest'

import type { Result } from '../api/client'
import type { PendingChoice } from '../api/schemas'
import { callWatchingForChoices } from './watching'

const aChoice: PendingChoice = {
  choice_id: 'c1',
  title: 'Choose a weapon',
  available_options: ['Longbow', 'Longsword'],
  remaining_selections: 1,
  require_complete: true,
}

/** A promise this test decides when to settle. */
function held<T>(): { promise: Promise<T>; settle: (value: T) => void } {
  let settle!: (value: T) => void
  const promise = new Promise<T>((resolve) => {
    settle = resolve
  })
  return { promise, settle }
}

const immediately = async (): Promise<void> => {}

describe('callWatchingForChoices', () => {
  it('hands back what the call produced when nothing was asked', async () => {
    const answer = await callWatchingForChoices(
      async () => ({ ok: true, data: 'done' }) as Result<string>,
      async () => ({ ok: true, data: [] }),
      () => {},
      immediately,
    )

    expect(answer).toEqual({ ok: true, data: 'done' })
  })

  it('reports a choice while the call is still waiting on it', async () => {
    const call = held<Result<string>>()
    const seen: PendingChoice[][] = []
    let polls = 0

    const answer = callWatchingForChoices(
      () => call.promise,
      async () => {
        polls += 1
        return polls === 1 ? { ok: true, data: [] } : { ok: true, data: [aChoice] }
      },
      (choices) => {
        seen.push(choices)
        if (choices.length > 0) {
          call.settle({ ok: true, data: 'unblocked' })
        }
      },
      immediately,
    )

    expect(await answer).toEqual({ ok: true, data: 'unblocked' })
    expect(seen.at(-1)).toEqual([aChoice])
  })

  it('does not make a call that asks nothing wait for a poll', async () => {
    const poll = vi.fn(async () => ({ ok: true, data: [] }) as Result<PendingChoice[]>)
    const neverFinishes = (): Promise<void> => new Promise(() => {})

    const answer = await callWatchingForChoices(
      async () => ({ ok: true, data: 'done' }) as Result<string>,
      poll,
      () => {},
      neverFinishes,
    )

    expect(answer).toEqual({ ok: true, data: 'done' })
    expect(poll).not.toHaveBeenCalled()
  })

  it('stops asking once the call is done', async () => {
    const poll = vi.fn(async () => ({ ok: true, data: [] }) as Result<PendingChoice[]>)

    await callWatchingForChoices(
      async () => ({ ok: true, data: 'done' }) as Result<string>,
      poll,
      () => {},
      immediately,
    )
    await new Promise((resolve) => setTimeout(resolve, 5))
    const pollsWhenDone = poll.mock.calls.length

    await new Promise((resolve) => setTimeout(resolve, 10))

    expect(poll.mock.calls.length).toBe(pollsWhenDone)
  })

  it('carries a failed call through untouched', async () => {
    const answer = await callWatchingForChoices(
      async () => ({ ok: false, error: { kind: 'NotAllowed', message: 'no' } }) as Result<string>,
      async () => ({ ok: true, data: [] }),
      () => {},
      immediately,
    )

    expect(answer).toEqual({ ok: false, error: { kind: 'NotAllowed', message: 'no' } })
  })

  it('keeps going when asking for choices itself fails', async () => {
    const answer = await callWatchingForChoices(
      async () => ({ ok: true, data: 'done' }) as Result<string>,
      async () => ({ ok: false, error: { kind: 'ServerUnreachable', message: 'down' } }),
      () => {},
      immediately,
    )

    expect(answer).toEqual({ ok: true, data: 'done' })
  })
})
