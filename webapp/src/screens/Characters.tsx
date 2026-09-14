import { useState } from 'react'

import { closeCharacter, createCharacter, isDirty, openCharacter, saveCharacter } from '../api/operations'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'

/** Create, open, save and close. The server holds the characters; this names them. */
export function Characters() {
  const { characterId, character, act, open } = useSession()
  const [name, setName] = useState('')
  const [path, setPath] = useState('')
  const [unsaved, setUnsaved] = useState<boolean | null>(null)

  const create = async (): Promise<void> => {
    const answer = await act(() => createCharacter(name || 'Sans nom'))
    if (answer.ok) {
      open(answer.data.character_id)
      setName('')
    }
  }

  const openFile = async (): Promise<void> => {
    const answer = await act(() => openCharacter(path))
    if (answer.ok) {
      open(answer.data.character_id)
    }
  }

  return (
    <>
      <Panel title="Nouveau personnage">
        <div className="row">
          <input
            type="text"
            value={name}
            placeholder="nom"
            onChange={(event) => setName(event.target.value)}
          />
          <button type="button" onClick={create}>
            Créer
          </button>
        </div>
      </Panel>

      <Panel title="Ouvrir un fichier">
        <div className="row">
          <input
            type="text"
            value={path}
            placeholder="chemin du .pcg"
            onChange={(event) => setPath(event.target.value)}
          />
          <button type="button" disabled={!path} onClick={openFile}>
            Ouvrir
          </button>
        </div>
        <p className="muted">
          Le chemin est lu par le serveur, pas par le navigateur : il désigne un fichier de la machine
          où PCGen tourne.
        </p>
      </Panel>

      {characterId ? (
        <Panel title={`Personnage ouvert : ${character?.name ?? characterId}`}>
          <div className="row">
            <button type="button" onClick={() => act(() => saveCharacter(characterId))}>
              Enregistrer
            </button>
            <button
              type="button"
              onClick={async () => {
                const answer = await isDirty(characterId)
                setUnsaved(answer.ok ? answer.data.dirty : null)
              }}
            >
              Modifications en attente ?
            </button>
            <button
              type="button"
              className="link"
              onClick={async () => {
                await act(() => closeCharacter(characterId))
                open(null)
              }}
            >
              Fermer
            </button>
          </div>
          {unsaved === null ? null : (
            <p className={unsaved ? 'warn' : 'done'}>
              {unsaved ? 'Des modifications ne sont pas enregistrées.' : 'Tout est enregistré.'}
            </p>
          )}
        </Panel>
      ) : null}
    </>
  )
}
