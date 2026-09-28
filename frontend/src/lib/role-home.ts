import type { Role } from '../types/api'

export function roleHome(role: Role): string {
  if (role === 'ADMIN') return '/admin/dashboard'
  if (role === 'FACULTY') return '/faculty/dashboard'
  return '/student/dashboard'
}
