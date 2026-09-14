import { useEffect, useState } from 'react'

import { listGameModes, listSources, loadSources } from '../api/operations'
import type { GameMode, NamedEntry } from '../api/schemas'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'

/**
 * Choose a game mode and its campaigns, then load them.
 *
 * A realistic set takes around half a minute to load, and nothing else is
 * possible until it has: without sources, PCGen refuses to make a character.
 */
export function Sources() {
  const { act, report } = useSession()
  const [modes, setModes] = useState<GameMode[]>([])
  const [mode, setMode] = useState('')
  const [campaigns, setCampaigns] = useState<NamedEntry[]>([])
  const [chosen, setChosen] = useState<string[]>([])
  const [loaded, setLoaded] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    listGameModes().then((answer) => (answer.ok ? setModes(answer.data) : report(answer.error)))
  }, [report])

  useEffect(() => {
    if (!mode) {
      setCampaigns([])
      return
    }
    listSources(mode).then((answer) => (answer.ok ? setCampaigns(answer.data) : report(answer.error)))
  }, [mode, report])

  const toggle = (key: string): void =>
    setChosen((keys) => (keys.includes(key) ? keys.filter((one) => one !== key) : [...keys, key]))

  const load = async (): Promise<void> => {
    setLoading(true)
    const answer = await act(() => loadSources(mode, chosen))
    setLoading(false)
    if (answer.ok) {
      setLoaded(answer.data.source_set_id)
    }
  }

  return (
    <Panel title="Sources">
      <label>
        Mode de jeu
        <select value={mode} disabled={loading} onChange={(event) => setMode(event.target.value)}>
          <option value="">—</option>
          {modes.map((one) => (
            <option key={one.name} value={one.name}>
              {one.display_name}
            </option>
          ))}
        </select>
      </label>

      {campaigns.length > 0 ? (
        <ul className="checklist">
          {campaigns.map((campaign) => (
            <li key={campaign.key}>
              <label>
                <input
                  type="checkbox"
                  checked={chosen.includes(campaign.key)}
                  disabled={loading}
                  onChange={() => toggle(campaign.key)}
                />
                {campaign.name}
              </label>
            </li>
          ))}
        </ul>
      ) : null}

      <div className="row">
        <button type="button" disabled={loading || !mode || chosen.length === 0} onClick={load}>
          Charger {chosen.length > 0 ? `(${chosen.length})` : ''}
        </button>
        {loading ? <span className="muted">chargement, une trentaine de secondes pour un jeu complet…</span> : null}
      </div>

      {loaded ? <p className="done">Chargé : {loaded}</p> : null}
    </Panel>
  )
}
