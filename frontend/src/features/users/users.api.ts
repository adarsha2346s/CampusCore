import { http } from '../../lib/api/http'
import type { UserRequest, UserResponse } from '../../types/api'

export const userKeys = {
  all: ['admin', 'users'] as const,
  detail: (id: number) => ['admin', 'users', id] as const,
}

export const getUsers = () => http<UserResponse[]>('users')
export const getUser = (id: number) => http<UserResponse>(`users/${id}`)
export const createUser = (request: UserRequest) => http<UserResponse>('users', { method: 'POST', body: request })
export const updateUser = (id: number, request: UserRequest) => http<UserResponse>(`users/${id}`, { method: 'PUT', body: request })
export const deactivateUser = (id: number) => http<void>(`users/${id}`, { method: 'DELETE' })
