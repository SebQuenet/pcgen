import { useState } from 'react'

import { addClassLevel } from '../api/operations'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'
import { Picker } from '../ui/Picker'

/**
 * Add class levels.
 *
 * PCGen may take fewer levels than asked, or take them and still complain, so
 * levels are added one at a time by default and the summary above is the truth.
 */
export function Levels() {
  const { characterId, character, act } = useSession()
  const [howMany, setHowMany] = useState(1)

  if (!characterId) {
    return null
  }

  return (
    <>
      <Panel title="Niveaux">
        {character && character.classes.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Classe</th>
                <th>Niveau</th>
              </tr>
            </thead>
            <tbody>
              {character.classes.map((one) => (
                <tr key={one.name}>
                  <td>{one.name}</td>
                  <td>{one.level}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="muted">aucun niveau de classe</p>
        )}
        <p className="facts-inline">
          PV {character?.hp ?? 0} — XP {character?.xp ?? 0} / {character?.xp_for_next_level ?? 0}
        </p>
      </Panel>

      <Panel title="Ajouter des niveaux">
        <label>
          Combien
          <input
            type="number"
            min={1}
            max={20}
            value={howMany}
            onChange={(event) => setHowMany(Math.max(1, Number(event.target.value)))}
          />
        </label>
        <Picker
          kind="class"
          label="Classe"
          onPick={(entry) => act(() => addClassLevel(characterId, entry.key, howMany))}
        />
      </Panel>
    </>
  )
}
