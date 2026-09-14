import { useCallback, useEffect, useMemo, useState } from 'react'

import type { Failure, Result } from './api/client'
import { getCharacter, getPendingChoices } from './api/operations'
import type { CharacterSummary, PendingChoice } from './api/schemas'
import { ChoicePrompt } from './choices/ChoicePrompt'
import { callWatchingForChoices } from './choices/watching'
import { Abilities } from './screens/Abilities'
import { Characters } from './screens/Characters'
import { Equipment } from './screens/Equipment'
import { Feats } from './screens/Feats'
import { Identity } from './screens/Identity'
import { Levels } from './screens/Levels'
import { Sheet } from './screens/Sheet'
import { Skills } from './screens/Skills'
import { Sources } from './screens/Sources'
import { Spells } from './screens/Spells'
import { SessionContext, type Session } from './state/session'
import { Notice } from './ui/Notice'

const SCREENS = [
  { id: 'sources', label: 'Sources', needsCharacter: false, render: () => <Sources /> },
  { id: 'characters', label: 'Personnages', needsCharacter: false, render: () => <Characters /> },
  { id: 'identity', label: 'Identité', needsCharacter: true, render: () => <Identity /> },
  { id: 'levels', label: 'Niveaux', needsCharacter: true, render: () => <Levels /> },
  { id: 'abilities', label: 'Caractéristiques', needsCharacter: true, render: () => <Abilities /> },
  { id: 'skills', label: 'Compétences', needsCharacter: true, render: () => <Skills /> },
  { id: 'feats', label: 'Dons', needsCharacter: true, render: () => <Feats /> },
  { id: 'equipment', label: 'Équipement', needsCharacter: true, render: () => <Equipment /> },
  { id: 'spells', label: 'Sorts', needsCharacter: true, render: () => <Spells /> },
  { id: 'sheet', label: 'Fiche', needsCharacter: true, render: () => <Sheet /> },
] as const

export function App() {
  const [screenId, setScreenId] = useState<string>('sources')
  const [characterId, setCharacterId] = useState<string | null>(null)
  const [character, setCharacter] = useState<CharacterSummary | null>(null)
  const [waiting, setWaiting] = useState<PendingChoice | null>(null)
  const [failure, setFailure] = useState<Failure | null>(null)
  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async (): Promise<void> => {
    if (!characterId) {
      setCharacter(null)
      return
    }
    const summary = await getCharacter(characterId)
    if (summary.ok) {
      setCharacter(summary.data)
    } else {
      setFailure(summary.error)
    }
  }, [characterId])

  // Opening a character is the one change nothing else reacts to, so the summary
  // is read here whenever the id changes.
  useEffect(() => {
    void refresh()
  }, [refresh])

  const act = useCallback(
    async <T,>(call: () => Promise<Result<T>>): Promise<Result<T>> => {
      setBusy(true)
      setFailure(null)
      const answer = await callWatchingForChoices(call, getPendingChoices, (choices) =>
        setWaiting(choices[0] ?? null),
      )
      setWaiting(null)
      setBusy(false)
      if (!answer.ok) {
        setFailure(answer.error)
      }
      await refresh()
      return answer
    },
    [refresh],
  )

  const session: Session = useMemo(
    () => ({ characterId, character, busy, act, refresh, open: setCharacterId, report: setFailure }),
    [characterId, character, busy, act, refresh],
  )

  const screen = SCREENS.find((one) => one.id === screenId) ?? SCREENS[0]
  const blocked = screen.needsCharacter && !characterId

  return (
    <SessionContext.Provider value={session}>
      <header className="top">
        <h1>PCGen</h1>
        <p className="who">
          {character ? (
            <>
              <strong>{character.name}</strong>
              {character.race ? ` — ${character.race}` : ''}
              {character.classes.length > 0
                ? ` — ${character.classes.map((one) => `${one.name} ${one.level}`).join(', ')}`
                : ''}
              {character.todo_list.length > 0 ? (
                <span className="todo" title={character.todo_list.join('\n')}>
                  {character.todo_list.length} à faire
                </span>
              ) : null}
            </>
          ) : (
            <span className="muted">aucun personnage ouvert</span>
          )}
        </p>
        {busy ? <span className="busy">PCGen travaille…</span> : null}
      </header>

      <nav className="tabs">
        {SCREENS.map((one) => (
          <button
            key={one.id}
            type="button"
            className={one.id === screen.id ? 'current' : ''}
            disabled={one.needsCharacter && !characterId}
            onClick={() => setScreenId(one.id)}
          >
            {one.label}
          </button>
        ))}
      </nav>

      {failure ? <Notice failure={failure} onDismiss={() => setFailure(null)} /> : null}

      <main>
        {blocked ? <p className="muted">Créez ou ouvrez un personnage d'abord.</p> : screen.render()}
      </main>

      {waiting ? <ChoicePrompt choice={waiting} onAnswered={() => setWaiting(null)} /> : null}
    </SessionContext.Provider>
  )
}
