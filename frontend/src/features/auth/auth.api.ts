import type { LoginRequest, LoginResponse, MeResponse } from '../../types/api'
import { http } from '../../lib/api/http'

export function loginRequest(credentials: LoginRequest): Promise<LoginResponse> {
  return http<LoginResponse>('auth/login', {
    method: 'POST',
    body: credentials,
    authenticated: false,
    handleUnauthorized: false,
  })
}

export function getCurrentUser(): Promise<MeResponse> {
  return http<MeResponse>('auth/me')
}
