export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>
  readonly payload: unknown

  constructor(
    status: number,
    message: string,
    payload: unknown,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
    this.fieldErrors = fieldErrors
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function normalizeApiError(status: number, payload: unknown, authenticationAttempt = false): ApiError {
  if (payload instanceof ApiError) return payload

  const data = isRecord(payload) ? payload : {}
  const nestedErrors = isRecord(data.errors) ? data.errors : undefined
  const directFields = isRecord(payload) && !('error' in data) && !('message' in data)
    ? data
    : undefined
  const fieldSource = nestedErrors ?? directFields ?? {}
  const fieldErrors = Object.fromEntries(
    Object.entries(fieldSource).filter(
      (entry): entry is [string, string] => typeof entry[1] === 'string',
    ),
  )
  const candidateMessage = typeof data.message === 'string' ? data.message.trim() : ''
  const isImplementationDetail = /\b(?:Exception|Error)\b|(?:^|\n)\s*at\s+[\w.$]+|org\.springframework|com\.campuscore|java\./i.test(candidateMessage)

  const message = status === 401
    ? authenticationAttempt
      ? 'Invalid username or password.'
      : 'Your session is no longer valid. Please sign in again.'
    : status === 403
      ? 'You do not have permission to perform this action.'
      : status >= 500
      ? 'The server could not complete this request. Please try again later.'
      : (!isImplementationDetail && candidateMessage) ||
          'The request could not be completed. Check the details and try again.'

  return new ApiError(status, message, payload, fieldErrors)
}
