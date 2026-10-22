import { http } from '../../lib/api/http'
import type {
  EnrollmentResponse,
  PageResponse,
  StudentRequest,
  StudentResponse,
  StudentSelfResponse,
} from '../../types/api'

export const studentKeys = {
  all: ['admin', 'students'] as const,
  detail: (id: number) => ['admin', 'students', id] as const,
  enrollments: (id: number) => ['admin', 'students', id, 'enrollments'] as const,
  self: ['student', 'self'] as const,
  page: (page: number, size: number) => ['admin', 'students', 'page', page, size] as const,
}

export const getStudents = () => http<StudentResponse[]>('students')
export const getStudentsPage = (page: number, size: number) => http<PageResponse<StudentResponse>>('students', { query: { page, size } })
export const getMyStudentProfile = () => http<StudentSelfResponse>('students/me')
export const getStudent = (id: number) => http<StudentResponse>(`students/${id}`)
/**
 * A student's enrollments, scoped to the caller.
 *
 * <p>An administrator receives every enrollment the student holds; a faculty
 * member receives only the ones in the courses they currently teach. The scope
 * is applied by the server, so the caller never receives the courses it is not
 * entitled to see and never has to filter the result itself.
 */
export const getStudentEnrollments = (id: number) =>
  http<EnrollmentResponse[]>(`students/${id}/enrollments`)
export const createStudent = (request: StudentRequest) => http<StudentResponse>('students', { method: 'POST', body: request })
export const updateStudent = (id: number, request: StudentRequest) => http<StudentResponse>(`students/${id}`, { method: 'PUT', body: request })
export const deactivateStudent = (id: number) => http<void>(`students/${id}`, { method: 'DELETE' })
