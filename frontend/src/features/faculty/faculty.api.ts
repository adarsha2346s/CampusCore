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

export interface FacultyAssignment {
  assignmentId: number
  courseId: number
  courseCode: string
  courseName: string
  facultyId: number
  employeeNumber: string
  facultyName: string
  assignedAt: string
  endedAt: string | null
  active: boolean
}

export const facultyCourseKeys = {
  mine: (facultyId: number) => ['faculty', 'courses', facultyId] as const,
}

/**
 * Courses currently assigned to a faculty member.
 *
 * Read-only here. Assignment writes are an administrator capability and are not
 * exposed in this workspace, mirroring the backend role matrix.
 */
export const getFacultyCourses = (facultyId: number) =>
  http<FacultyAssignment[]>(`faculty/${facultyId}/courses`)
