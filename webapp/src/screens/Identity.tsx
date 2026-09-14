import { useEffect, useState } from 'react'

import {
  getBiography,
  setAlignment,
  setBiography,
  setDeity,
  setName as renameCharacter,
  setRace,
} from '../api/operations'
import type { Biography } from '../api/schemas'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'
import { Picker } from '../ui/Picker'

const NOTES = [
  ['bio', 'Biographie'],
  ['description', 'Description'],
  ['companions', 'Compagnons'],
  ['assets', 'Biens'],
  ['magic', 'Magie'],
  ['gm_notes', 'Notes du MJ'],
] as const

/** Name, race, alignment, deity, and the prose kept about a character. */
export function Identity() {
  const { characterId, character, act } = useSession()
  const [name, typeName] = useState('')
  const [biography, readBiography] = useState<Biography | null>(null)
  const [notes, setNotes] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!characterId) {
      return
    }
    getBiography(characterId).then((answer) => {
      if (answer.ok) {
        readBiography(answer.data)
        setNotes(Object.fromEntries(NOTES.map(([field]) => [field, answer.data[field] ?? ''])))
      }
    })
  }, [characterId])

  if (!characterId) {
    return null
  }

  return (
    <>
      <Panel title="Identité">
        <div className="row">
          <input
            type="text"
            value={name}
            placeholder={character?.name ?? 'nom'}
            onChange={(event) => typeName(event.target.value)}
          />
          <button type="button" disabled={!name} onClick={() => act(() => renameCharacter(characterId, name))}>
            Renommer
          </button>
        </div>
        <dl className="facts">
          <dt>Race</dt>
          <dd>{character?.race ?? '—'}</dd>
          <dt>Alignement</dt>
          <dd>{character?.alignment ?? '—'}</dd>
          <dt>Divinité</dt>
          <dd>{character?.deity ?? '—'}</dd>
        </dl>
      </Panel>

      <div className="columns">
        <Panel title="Race">
          <Picker kind="race" label="Race" onPick={(entry) => act(() => setRace(characterId, entry.key))} />
        </Panel>
        <Panel title="Alignement">
          <Picker
            kind="alignment"
            label="Alignement"
            onPick={(entry) => act(() => setAlignment(characterId, entry.key))}
          />
        </Panel>
        <Panel title="Divinité">
          <Picker kind="deity" label="Divinité" onPick={(entry) => act(() => setDeity(characterId, entry.key))} />
        </Panel>
      </div>

      <Panel
        title="Notes"
        actions={
          <button type="button" onClick={() => act(() => setBiography(characterId, notes))}>
            Enregistrer les notes
          </button>
        }
      >
        {biography ? (
          <div className="notes">
            {NOTES.map(([field, label]) => (
              <label key={field}>
                {label}
                <textarea
                  value={notes[field] ?? ''}
                  rows={3}
                  onChange={(event) => setNotes({ ...notes, [field]: event.target.value })}
                />
              </label>
            ))}
          </div>
        ) : (
          <p className="muted">lecture…</p>
        )}
      </Panel>
    </>
  )
}
