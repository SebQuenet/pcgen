import { useEffect, useState } from 'react'

import { addAbility, listAbilities, listAbilityCategories, removeAbility } from '../api/operations'
import type { AbilityCategory, AvailableAbility } from '../api/schemas'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'

/**
 * Feats, traits and class abilities, category by category.
 *
 * An ability that asks a question comes back with the options instead of being
 * added; picking one and sending it again is what takes it.
 */
export function Feats() {
  const { characterId, character, act } = useSession()
  const [categories, setCategories] = useState<AbilityCategory[]>([])
  const [categoryKey, setCategoryKey] = useState('')
  const [abilities, setAbilities] = useState<AvailableAbility[]>([])
  const [search, setSearch] = useState('')
  const [onlyQualified, setOnlyQualified] = useState(true)
  const [asking, setAsking] = useState<{ ability: string; options: string[] } | null>(null)

  useEffect(() => {
    if (characterId) {
      listAbilityCategories(characterId).then((answer) => {
        if (answer.ok) {
          setCategories(answer.data)
          setCategoryKey((current) => current || (answer.data[0]?.key ?? ''))
        }
      })
    }
  }, [characterId, character])

  useEffect(() => {
    if (characterId && categoryKey) {
      listAbilities(characterId, categoryKey).then((answer) => setAbilities(answer.ok ? answer.data : []))
    }
  }, [characterId, categoryKey, character])

  if (!characterId) {
    return null
  }

  const take = async (abilityKey: string, choice: string[]): Promise<void> => {
    const answer = await act(() => addAbility(characterId, categoryKey, abilityKey, choice))
    if (answer.ok && answer.data.status === 'choice_required') {
      setAsking({ ability: abilityKey, options: answer.data.available_choices })
    } else {
      setAsking(null)
    }
  }

  const shown = abilities.filter(
    (ability) =>
      (!onlyQualified || ability.qualified) &&
      (search === '' || ability.name.toLowerCase().includes(search.toLowerCase())),
  )

  return (
    <>
      <Panel title="Catégories">
        <ul className="chips">
          {categories.map((category) => (
            <li key={category.key}>
              <button
                type="button"
                className={category.key === categoryKey ? 'current' : ''}
                onClick={() => setCategoryKey(category.key)}
              >
                {category.name} <span className="count">{category.remaining}/{category.total}</span>
              </button>
            </li>
          ))}
        </ul>
      </Panel>

      {asking ? (
        <Panel title={`${asking.ability} demande un choix`}>
          <ul className="options">
            {asking.options.map((option) => (
              <li key={option}>
                <button type="button" onClick={() => take(asking.ability, [option])}>
                  {option}
                </button>
              </li>
            ))}
          </ul>
        </Panel>
      ) : null}

      <Panel title="Disponibles">
        <div className="row">
          <input
            type="search"
            value={search}
            placeholder="chercher…"
            onChange={(event) => setSearch(event.target.value)}
          />
          <label>
            <input
              type="checkbox"
              checked={onlyQualified}
              onChange={() => setOnlyQualified(!onlyQualified)}
            />
            seulement celles auxquelles il a droit
          </label>
        </div>
        <ul className="list">
          {shown.slice(0, 120).map((ability) => (
            <li key={ability.key}>
              <button type="button" onClick={() => take(ability.key, [])}>
                {ability.name}
              </button>
              <button
                type="button"
                className="link"
                onClick={() => act(() => removeAbility(characterId, categoryKey, ability.key))}
              >
                retirer
              </button>
            </li>
          ))}
        </ul>
        {shown.length > 120 ? <p className="muted">{shown.length - 120} de plus, affinez la recherche</p> : null}
      </Panel>
    </>
  )
}
