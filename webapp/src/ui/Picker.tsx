import { useEffect, useState } from 'react'

import { listDatasetEntries } from '../api/operations'
import type { NamedEntry } from '../api/schemas'

/**
 * Pick something out of the loaded data by typing part of its name.
 *
 * A source set holds thousands of entries, so the list is asked for as the
 * person types rather than loaded whole.
 */
export function Picker({
  kind,
  label,
  onPick,
  disabled,
}: {
  kind: string
  label: string
  onPick: (entry: NamedEntry) => void
  disabled?: boolean
}) {
  const [search, setSearch] = useState('')
  const [entries, setEntries] = useState<NamedEntry[]>([])
  const [looking, setLooking] = useState(false)

  useEffect(() => {
    let current = true
    setLooking(true)
    const timer = setTimeout(async () => {
      const found = await listDatasetEntries(kind, search || undefined, 40)
      if (current) {
        setEntries(found.ok ? found.data : [])
        setLooking(false)
      }
    }, 200)
    return () => {
      current = false
      clearTimeout(timer)
    }
  }, [kind, search])

  return (
    <div className="picker">
      <label>
        {label}
        <input
          type="search"
          value={search}
          placeholder="chercher…"
          disabled={disabled}
          onChange={(event) => setSearch(event.target.value)}
        />
      </label>
      <ul>
        {looking && entries.length === 0 ? <li className="muted">recherche…</li> : null}
        {entries.map((entry) => (
          <li key={entry.key}>
            <button type="button" disabled={disabled} onClick={() => onPick(entry)}>
              {entry.name}
            </button>
          </li>
        ))}
        {!looking && entries.length === 0 ? <li className="muted">rien de ce nom</li> : null}
      </ul>
    </div>
  )
}
