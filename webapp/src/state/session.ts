import { createContext, useContext } from 'react'

import type { Failure, Result } from '../api/client'
import type { CharacterSummary } from '../api/schemas'

/** What every screen needs: who is open, and how to act on them. */
export type Session = {
  readonly characterId: string | null
  readonly character: CharacterSummary | null
  readonly busy: boolean
  /** Run an operation, watching for anything PCGen stops to ask, then refresh. */
  readonly act: <T>(call: () => Promise<Result<T>>) => Promise<Result<T>>
  readonly refresh: () => Promise<void>
  /** Make this character the one every screen works on, or none. */
  readonly open: (characterId: string | null) => void
  readonly report: (failure: Failure) => void
}

export const SessionContext = createContext<Session | null>(null)

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (!session) {
    throw new Error('A screen was rendered outside the session it needs')
  }
  return session
}
