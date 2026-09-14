import { useEffect, useState } from 'react'

import { getCharacterDetails, rollStats, setAllAbilityScores } from '../api/operations'
import type { CharacterDetails } from '../api/schemas'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'

const SCORES = ['STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA'] as const

/** The six scores, typed in or rolled, and what each one is made of. */
export function Abilities() {
  const { characterId, character, act } = useSession()
  const [typed, setTyped] = useState<Record<string, number>>({})
  const [details, setDetails] = useState<CharacterDetails | null>(null)

  useEffect(() => {
    setTyped(character?.ability_scores ?? {})
  }, [character])

  useEffect(() => {
    if (characterId) {
      getCharacterDetails(characterId).then((answer) => setDetails(answer.ok ? answer.data : null))
    }
  }, [characterId, character])

  if (!characterId) {
    return null
  }

  return (
    <>
      <Panel
        title="Caractéristiques"
        actions={
          <div className="row">
            <button type="button" onClick={() => act(() => setAllAbilityScores(characterId, typed))}>
              Appliquer
            </button>
            <button type="button" className="link" onClick={() => act(() => rollStats(characterId))}>
              Tirer aux dés
            </button>
          </div>
        }
      >
        <div className="scores">
          {SCORES.map((score) => (
            <label key={score}>
              {score}
              <input
                type="number"
                min={1}
                max={50}
                value={typed[score] ?? 10}
                onChange={(event) => setTyped({ ...typed, [score]: Number(event.target.value) })}
              />
              <span className="modifier">
                {(character?.ability_modifiers[score] ?? 0) >= 0 ? '+' : ''}
                {character?.ability_modifiers[score] ?? 0}
              </span>
            </label>
          ))}
        </div>
      </Panel>

      {details ? (
        <Panel title="Détail">
          <table>
            <thead>
              <tr>
                <th>Carac.</th>
                <th>Base</th>
                <th>Total</th>
                <th>Mod.</th>
                <th>Race</th>
                <th>Autre</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(details.stats).map(([key, stat]) => (
                <tr key={key}>
                  <td>{key}</td>
                  <td>{stat.base}</td>
                  <td>{stat.total}</td>
                  <td>{stat.modifier}</td>
                  <td>{stat.race_bonus}</td>
                  <td>{stat.other_bonus}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="facts-inline">
            PV {details.hp} — charge {details.carried_weight} ({details.load}, limite {details.weight_limit}) —
            bourse {details.funds} po — patrimoine {details.wealth} po
          </p>
        </Panel>
      ) : null}
    </>
  )
}
