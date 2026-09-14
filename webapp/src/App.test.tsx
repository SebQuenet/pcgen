// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { App } from './App'

/** A server that answers every operation with an empty list. */
function serverAnswersEmpty(): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify({ ok: true, data: [], error: null }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
    ),
  )
}

beforeEach(serverAnswersEmpty)

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('the application', () => {
  it('opens on the sources screen', () => {
    render(<App />)

    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('PCGen')
    expect(screen.getByRole('heading', { name: 'Sources' })).toBeDefined()
  })

  it('says no character is open, and keeps the character screens shut', () => {
    render(<App />)

    expect(screen.getByText('aucun personnage ouvert')).toBeDefined()
    for (const label of ['Identité', 'Niveaux', 'Compétences', 'Dons', 'Équipement', 'Sorts', 'Fiche']) {
      expect(screen.getByRole('button', { name: label }).hasAttribute('disabled')).toBe(true)
    }
  })

  it('leaves the screens that work without a character open', () => {
    render(<App />)

    for (const label of ['Sources', 'Personnages']) {
      expect(screen.getByRole('button', { name: label }).hasAttribute('disabled')).toBe(false)
    }
  })

  it('opens the rest of the screens once a character exists', async () => {
    answersByOperation({
      create_character: { character_id: 'abc' },
      get_character: {
        name: 'Miros',
        race: 'Human',
        alignment: null,
        deity: null,
        ability_scores: {},
        ability_modifiers: {},
        classes: [{ name: 'Fighter', level: 2 }],
        hp: 18,
        xp: 0,
        xp_for_next_level: 2000,
        carried_weight: '0',
        load: 'Light',
        todo_list: ['in_featTodoRemain'],
      },
    })
    render(<App />)

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Personnages' }))
    })
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Créer' }))
    })

    expect(screen.getByText('Miros')).toBeDefined()
    expect(screen.getByText('1 à faire')).toBeDefined()
    expect(screen.getByRole('button', { name: 'Niveaux' }).hasAttribute('disabled')).toBe(false)
  })
})

/** A server that answers each named operation with its own payload, and [] otherwise. */
function answersByOperation(byName: Record<string, unknown>): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string) => {
      const name = url.replace('/api/', '')
      return new Response(JSON.stringify({ ok: true, data: byName[name] ?? [], error: null }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }),
  )
}
