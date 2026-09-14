import { z, type ZodType } from 'zod'

/** Why a call did not produce what was asked for. */
export type Failure = {
  readonly kind: string
  readonly message: string
}

/**
 * What a call gives back. Failure is part of the type, so nothing thrown by the
 * network or by an unexpected payload escapes into the interface.
 */
export type Result<T> = { readonly ok: true; readonly data: T } | { readonly ok: false; readonly error: Failure }

/** The envelope every PCGen answer arrives in. */
const envelope = z.object({
  ok: z.boolean(),
  data: z.unknown().nullable(),
  error: z.object({ kind: z.string(), message: z.string() }).nullable(),
})

const UNREADABLE = 'UnreadableResponse'
const UNREACHABLE = 'ServerUnreachable'

/**
 * Call one PCGen operation.
 *
 * @param name the operation, in snake_case, as GET /api/operations lists it
 * @param args its arguments, matching the schema that listing declares
 * @param schema what the answer must look like for the caller to use it
 */
export async function callOperation<T>(name: string, args: object, schema: ZodType<T>): Promise<Result<T>> {
  let response: Response
  try {
    response = await fetch(`/api/${name}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(args),
    })
  } catch (reason) {
    return failed(UNREACHABLE, `PCGen did not answer: ${describe(reason)}`)
  }

  const body = await readEnvelope(response)
  if (!body.ok) {
    return body
  }
  if (!body.data.ok) {
    return failed(
      body.data.error?.kind ?? UNREADABLE,
      body.data.error?.message ?? `${name} failed without saying why`,
    )
  }

  const expected = schema.safeParse(body.data.data)
  return expected.success
    ? { ok: true, data: expected.data }
    : failed(UNREADABLE, `${name} answered with something unexpected: ${expected.error.issues[0]?.message ?? ''}`)
}

async function readEnvelope(response: Response): Promise<Result<z.infer<typeof envelope>>> {
  let parsed: unknown
  try {
    parsed = await response.json()
  } catch {
    return failed(UNREADABLE, `PCGen answered ${response.status} with something that is not JSON`)
  }
  const read = envelope.safeParse(parsed)
  return read.success ? { ok: true, data: read.data } : failed(UNREADABLE, 'PCGen answered outside its own envelope')
}

function failed(kind: string, message: string): Result<never> {
  return { ok: false, error: { kind, message } }
}

function describe(reason: unknown): string {
  return reason instanceof Error ? reason.message : String(reason)
}
