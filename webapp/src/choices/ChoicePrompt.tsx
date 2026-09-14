import { useState } from 'react'

import { resolveChoice } from '../api/operations'
import type { PendingChoice } from '../api/schemas'

/**
 * The question PCGen stopped to ask, put to the person.
 *
 * The call that opened it is still waiting on the server; posting the answer is
 * what lets it finish.
 */
export function ChoicePrompt({ choice, onAnswered }: { choice: PendingChoice; onAnswered: () => void }) {
  const [picked, setPicked] = useState<string[]>([])
  const [sending, setSending] = useState(false)

  const toggle = (option: string): void =>
    setPicked((chosen) =>
      chosen.includes(option)
        ? chosen.filter((one) => one !== option)
        : chosen.length < choice.remaining_selections
          ? [...chosen, option]
          : chosen,
    )

  const send = async (selections: string[]): Promise<void> => {
    setSending(true)
    await resolveChoice(choice.choice_id, selections)
    setSending(false)
    onAnswered()
  }

  return (
    <div className="choice" role="dialog" aria-label={choice.title}>
      <h3>{choice.title}</h3>
      <p className="muted">
        {picked.length} / {choice.remaining_selections} choisi(s)
      </p>
      <ul className="options">
        {choice.available_options.map((option) => (
          <li key={option}>
            <button
              type="button"
              className={picked.includes(option) ? 'picked' : ''}
              disabled={sending}
              onClick={() => toggle(option)}
            >
              {option}
            </button>
          </li>
        ))}
      </ul>
      <div className="row">
        <button type="button" disabled={sending || picked.length === 0} onClick={() => send(picked)}>
          Valider
        </button>
        <button type="button" className="link" disabled={sending} onClick={() => send([])}>
          Laisser PCGen choisir
        </button>
      </div>
    </div>
  )
}
