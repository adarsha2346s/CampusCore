import { http } from '../../lib/api/http'
import type { FacultyResponse, FacultySelfResponse, PageResponse } from '../../types/api'

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
  self: ['faculty', 'self'] as const,
  page: (page: number, size: number) => ['admin', 'faculty', 'page', page, size] as const,
}

export const getFaculty = () => http<FacultyResponse[]>('faculty')
export const getFacultyPage = (page: number, size: number) => http<PageResponse<FacultyResponse>>('faculty', { query: { page, size } })
export const getMyFacultyProfile = () => http<FacultySelfResponse>('faculty/me')
export const getFacultyMember = (id: number) => http<FacultyResponse>(`faculty/${id}`)
export const createFaculty = (request: FacultyCreateRequest) => http<FacultyResponse>('faculty', {
  method: 'POST',
  query: { ...request },
})
