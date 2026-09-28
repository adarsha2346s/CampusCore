import { http } from '../../lib/api/http'
import type { FacultyResponse } from '../../types/api'

export interface FacultyCreateRequest {
  userId: number
  departmentId: number
  employeeNumber: string
  firstName: string
  lastName?: string
  phone?: string
}

export const facultyKeys = {
  all: ['admin', 'faculty'] as const,
  detail: (id: number) => ['admin', 'faculty', id] as const,
}

export const getFaculty = () => http<FacultyResponse[]>('faculty')
export const getFacultyMember = (id: number) => http<FacultyResponse>(`faculty/${id}`)
export const createFaculty = (request: FacultyCreateRequest) => http<FacultyResponse>('faculty', {
  method: 'POST',
  query: { ...request },
})
