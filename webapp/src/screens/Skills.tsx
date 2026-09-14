import { useEffect, useState } from 'react'

import { getSkillSummary, investSkillPoints } from '../api/operations'
import type { SkillSummary } from '../api/schemas'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'
import { Picker } from '../ui/Picker'

/** Skill points, spent level by level, because that is how PCGen counts them. */
export function Skills() {
  const { characterId, character, act } = useSession()
  const [summary, setSummary] = useState<SkillSummary | null>(null)
  const [levelIndex, setLevelIndex] = useState(0)
  const [points, setPoints] = useState(1)

  useEffect(() => {
    if (characterId) {
      getSkillSummary(characterId).then((answer) => setSummary(answer.ok ? answer.data : null))
    }
  }, [characterId, character])

  if (!characterId) {
    return null
  }

  const levels = summary?.levels ?? []
  const atLevel = levels[levelIndex]

  return (
    <>
      <Panel title="Points par niveau">
        {levels.length === 0 ? (
          <p className="muted">ajoutez un niveau de classe d'abord</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Niveau</th>
                <th>Classe</th>
                <th>Gagnés</th>
                <th>Dépensés</th>
                <th>Restants</th>
              </tr>
            </thead>
            <tbody>
              {levels.map((level, index) => (
                <tr key={level.level} className={index === levelIndex ? 'current' : ''}>
                  <td>
                    <button type="button" className="link" onClick={() => setLevelIndex(index)}>
                      {level.level}
                    </button>
                  </td>
                  <td>{level.character_class}</td>
                  <td>{level.gained_skill_points}</td>
                  <td>{level.spent_skill_points}</td>
                  <td>{level.remaining_skill_points}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Panel>

      {atLevel ? (
        <Panel title={`Investir au niveau ${atLevel.level} (${atLevel.remaining_skill_points} restants)`}>
          <label>
            Points
            <input
              type="number"
              min={1}
              max={Math.max(1, atLevel.remaining_skill_points)}
              value={points}
              onChange={(event) => setPoints(Math.max(1, Number(event.target.value)))}
            />
          </label>
          <Picker
            kind="skill"
            label="Compétence"
            onPick={(entry) => act(() => investSkillPoints(characterId, entry.key, points, levelIndex))}
          />
        </Panel>
      ) : null}

      <Panel title="Compétences formées">
        {summary && summary.trained_skills.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Compétence</th>
                <th>Rangs</th>
                <th>Mod.</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {summary.trained_skills.map((skill) => (
                <tr key={skill.key}>
                  <td>{skill.name}</td>
                  <td>{skill.ranks}</td>
                  <td>{skill.modifier}</td>
                  <td>{skill.total}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="muted">aucun rang investi</p>
        )}
      </Panel>
    </>
  )
}
