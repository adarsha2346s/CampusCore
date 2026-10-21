let accessToken: string | null = null
let unauthorizedHandler: (() => void) | undefined
let forbiddenHandler: (() => void) | undefined

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string): void {
  accessToken = token
}

export function clearSession(): void {
  accessToken = null
}

export function registerUnauthorizedHandler(handler: () => void): () => void {
  unauthorizedHandler = handler
  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = undefined
  }
}

/**
 * The API answers 403 when the session is valid but the caller's role changed or
 * the record is outside their scope. The shell uses this to resync the identity
 * so navigation cannot keep showing a role the server no longer honours.
 */
export function registerForbiddenHandler(handler: () => void): () => void {
  forbiddenHandler = handler
  return () => {
    if (forbiddenHandler === handler) forbiddenHandler = undefined
  }
}

export function handleForbidden(): void {
  forbiddenHandler?.()
}

export function handleUnauthorized(): void {
  clearSession()
  unauthorizedHandler?.()
}
