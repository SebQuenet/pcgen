import { useEffect, useState } from 'react'

import {
  addKnownSpell,
  addPreparedSpell,
  getAvailableSpells,
  getKnownSpells,
  getPreparedSpells,
} from '../api/operations'
import type { SpellEntry } from '../api/schemas'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'

/** What a caster may learn, knows, and has prepared. */
export function Spells() {
  const { characterId, character, act } = useSession()
  const [available, setAvailable] = useState<SpellEntry[]>([])
  const [known, setKnown] = useState<SpellEntry[]>([])
  const [prepared, setPrepared] = useState<SpellEntry[]>([])
  const [search, setSearch] = useState('')

  useEffect(() => {
    if (!characterId) {
      return
    }
    getAvailableSpells(characterId).then((answer) => setAvailable(answer.ok ? answer.data : []))
    getKnownSpells(characterId).then((answer) => setKnown(answer.ok ? answer.data : []))
    getPreparedSpells(characterId).then((answer) => setPrepared(answer.ok ? answer.data : []))
  }, [characterId, character])

  if (!characterId) {
    return null
  }

  if (available.length === 0 && known.length === 0) {
    return (
      <Panel title="Sorts">
        <p className="muted">Ce personnage ne lance pas de sorts, ou n'a pas encore de niveau de lanceur.</p>
      </Panel>
    )
  }

  const matching = available.filter(
    (spell) => search === '' || spell.name.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <>
      <Panel title="Disponibles">
        <input
          type="search"
          value={search}
          placeholder="chercher…"
          onChange={(event) => setSearch(event.target.value)}
        />
        <ul className="list">
          {matching.slice(0, 80).map((spell) => (
            <li key={`${spell.key}-${spell.level}-${spell.character_class}`}>
              <span>
                {spell.name} <span className="muted">niv. {spell.level} — {spell.character_class}</span>
              </span>
              <button type="button" onClick={() => act(() => addKnownSpell(characterId, spell.name))}>
                apprendre
              </button>
            </li>
          ))}
        </ul>
        {matching.length > 80 ? <p className="muted">{matching.length - 80} de plus, affinez la recherche</p> : null}
      </Panel>

      <div className="columns">
        <Panel title="Connus">
          <ul className="list">
            {known.map((spell) => (
              <li key={`${spell.key}-${spell.level}`}>
                <span>
                  {spell.name} <span className="muted">niv. {spell.level}</span>
                </span>
                <button type="button" onClick={() => act(() => addPreparedSpell(characterId, spell.name))}>
                  préparer
                </button>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Préparés">
          <ul className="list">
            {prepared.map((spell) => (
              <li key={`${spell.key}-${spell.spell_list}`}>
                <span>
                  {spell.name} {spell.count && spell.count > 1 ? `×${spell.count}` : ''}
                  <span className="muted"> {spell.spell_list}</span>
                </span>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </>
  )
}
