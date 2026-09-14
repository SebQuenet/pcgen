import type { Result } from '../api/client'
import type { PendingChoice } from '../api/schemas'

const POLL_INTERVAL_MS = 500

const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * Make a call that may stop to ask something, and watch for the question.
 *
 * An operation that opens a chooser holds the server's one worker until the
 * answer arrives, so the answer cannot be sent by the same request. While the
 * call is in flight this asks the server what it is waiting on, and hands
 * anything it finds to `onChoices` — whose job is to put the question to the
 * person and post the answer back.
 */
export async function callWatchingForChoices<T>(
  call: () => Promise<Result<T>>,
  poll: () => Promise<Result<PendingChoice[]>>,
  onChoices: (choices: PendingChoice[]) => void,
  wait: (ms: number) => Promise<void> = sleep,
): Promise<Result<T>> {
  let stillCalling = true
  const answer = call().finally(() => {
    stillCalling = false
  })

  // The watch is not awaited: a call that asks nothing would otherwise pay for a
  // poll interval it never needed. It stops on its own when the call is done.
  void (async () => {
    while (stillCalling) {
      await wait(POLL_INTERVAL_MS)
      if (!stillCalling) {
        return
      }
      const waiting = await poll()
      if (waiting.ok && waiting.data.length > 0) {
        onChoices(waiting.data)
      }
    }
  })().catch(() => undefined)

  return answer
}
