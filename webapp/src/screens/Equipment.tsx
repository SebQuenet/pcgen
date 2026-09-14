import { useEffect, useState } from 'react'

import {
  buyEquipment,
  equipItem,
  getEquippedItems,
  getInventory,
  sellEquipment,
  setFunds,
  unequipItem,
} from '../api/operations'
import type { EquippedSlot, Inventory } from '../api/schemas'
import { useSession } from '../state/session'
import { Panel } from '../ui/Panel'
import { Picker } from '../ui/Picker'

/** Buying, carrying and wearing. */
export function Equipment() {
  const { characterId, character, act } = useSession()
  const [inventory, setInventory] = useState<Inventory | null>(null)
  const [slots, setSlots] = useState<EquippedSlot[]>([])
  const [purse, setPurse] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [wearing, setWearing] = useState<string | null>(null)

  useEffect(() => {
    if (!characterId) {
      return
    }
    getInventory(characterId).then((answer) => setInventory(answer.ok ? answer.data : null))
    getEquippedItems(characterId).then((answer) => setSlots(answer.ok ? answer.data : []))
  }, [characterId, character])

  if (!characterId) {
    return null
  }

  const worn = slots.filter((slot) => slot.equipment !== null)
  const free = slots.filter((slot) => slot.equipment === null && slot.type === 'PHANTOM_SLOT')

  return (
    <>
      <Panel
        title={`Bourse : ${inventory?.funds ?? 0} po`}
        actions={
          <div className="row">
            <input
              type="number"
              value={purse}
              placeholder="montant"
              onChange={(event) => setPurse(event.target.value)}
            />
            <button type="button" disabled={purse === ''} onClick={() => act(() => setFunds(characterId, Number(purse)))}>
              Fixer
            </button>
          </div>
        }
      >
        <p className="facts-inline">
          Charge {inventory?.carried_weight ?? '0'} ({inventory?.load ?? '—'})
        </p>
      </Panel>

      <Panel title="Acheter">
        <label>
          Quantité
          <input
            type="number"
            min={1}
            value={quantity}
            onChange={(event) => setQuantity(Math.max(1, Number(event.target.value)))}
          />
        </label>
        <Picker
          kind="equipment"
          label="Objet"
          onPick={(entry) => act(() => buyEquipment(characterId, entry.key, quantity))}
        />
      </Panel>

      <Panel title="Inventaire">
        {inventory && inventory.items.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Objet</th>
                <th>Nb</th>
                <th>Prix</th>
                <th>Poids</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {inventory.items.map((item) => (
                <tr key={item.key}>
                  <td>{item.name}</td>
                  <td>{item.quantity}</td>
                  <td>{item.cost}</td>
                  <td>{item.weight}</td>
                  <td>
                    <button type="button" className="link" onClick={() => setWearing(item.key)}>
                      équiper
                    </button>
                    <button
                      type="button"
                      className="link"
                      onClick={() => act(() => sellEquipment(characterId, item.key, 1))}
                    >
                      vendre
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="muted">rien dans les sacs</p>
        )}
      </Panel>

      {wearing ? (
        <Panel title={`Où porter ${wearing} ?`}>
          <ul className="chips">
            {free.map((slot) => (
              <li key={slot.slot}>
                <button
                  type="button"
                  onClick={async () => {
                    await act(() => equipItem(characterId, wearing, slot.slot))
                    setWearing(null)
                  }}
                >
                  {slot.slot}
                </button>
              </li>
            ))}
          </ul>
          <button type="button" className="link" onClick={() => setWearing(null)}>
            annuler
          </button>
        </Panel>
      ) : null}

      <Panel title="Porté">
        {worn.length > 0 ? (
          <ul className="list">
            {worn.map((slot) => (
              <li key={`${slot.slot}-${slot.equipment_key}`}>
                <span>
                  {slot.slot} : {slot.equipment} {slot.quantity && slot.quantity > 1 ? `×${slot.quantity}` : ''}
                </span>
                <button
                  type="button"
                  className="link"
                  onClick={() => act(() => unequipItem(characterId, slot.slot))}
                >
                  retirer
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted">rien de porté</p>
        )}
      </Panel>
    </>
  )
}
