import { useEffect, useState } from 'react'

import { getCharacterSheet } from '../api/operations'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'

const FORMATS = ['markdown', 'toon', 'json', 'xml'] as const

/** The sheet as PCGen renders it, in whichever of its own formats. */
export function Sheet() {
  const { characterId, character, report } = useSession()
  const [format, setFormat] = useState<string>('markdown')
  const [content, setContent] = useState('')

  useEffect(() => {
    if (!characterId) {
      return
    }
    getCharacterSheet(characterId, format).then((answer) =>
      answer.ok ? setContent(answer.data.content) : report(answer.error),
    )
  }, [characterId, character, format, report])

  if (!characterId) {
    return null
  }

  return (
    <Panel
      title="Fiche"
      actions={
        <select value={format} onChange={(event) => setFormat(event.target.value)}>
          {FORMATS.map((one) => (
            <option key={one} value={one}>
              {one}
            </option>
          ))}
        </select>
      }
    >
      <pre className="sheet">{content || 'rendu…'}</pre>
    </Panel>
  )
}
