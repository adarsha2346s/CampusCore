import { ApiError, normalizeApiError } from './api-error'
import { getAccessToken, handleUnauthorized } from '../../features/auth/session'

const defaultBaseUrl = 'http://localhost:8080/api/v1'
const baseUrl = (import.meta.env.VITE_API_BASE_URL || defaultBaseUrl).replace(/\/$/, '')

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  query?: Record<string, string | number | boolean | null | undefined>
  body?: unknown
  authenticated?: boolean
  handleUnauthorized?: boolean
}

export async function http<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const {
    query,
    body,
    authenticated = true,
    handleUnauthorized: shouldHandleUnauthorized = authenticated,
    headers: suppliedHeaders,
    ...requestOptions
  } = options

  const url = new URL(`${baseUrl}/${path.replace(/^\//, '')}`)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null) {
      url.searchParams.set(key, String(value))
    }
  }

  const headers = new Headers(suppliedHeaders)
  headers.set('Accept', 'application/json')

  if (body !== undefined) headers.set('Content-Type', 'application/json')
  if (authenticated) {
    const token = getAccessToken()
    if (token) headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(url, {
      ...requestOptions,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'The server could not be reached. Check your connection and try again.', null)
  }

  let payload: unknown = null
  if (response.status !== 204) {
    const text = await response.text()
    if (text) {
      try {
        payload = JSON.parse(text) as unknown
      } catch {
        payload = text
      }
    }
  }

  if (!response.ok) {
    const error = normalizeApiError(response.status, payload, !authenticated && response.status === 401)
    if (response.status === 401 && shouldHandleUnauthorized) {
      handleUnauthorized()
    }
    throw error
  }

  return payload as T
}
