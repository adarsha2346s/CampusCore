let accessToken: string | null = null
let unauthorizedHandler: (() => void) | undefined

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

export function handleUnauthorized(): void {
  clearSession()
  unauthorizedHandler?.()
}
