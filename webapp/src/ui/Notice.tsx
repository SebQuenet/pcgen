import type { Failure } from '../api/client'

/** What went wrong, said once, where the action was. */
export function Notice({ failure, onDismiss }: { failure: Failure; onDismiss?: () => void }) {
  return (
    <p className="notice" role="alert">
      <strong>{failure.kind}</strong> {failure.message}
      {onDismiss ? (
        <button type="button" className="link" onClick={onDismiss}>
          fermer
        </button>
      ) : null}
    </p>
  )
}
