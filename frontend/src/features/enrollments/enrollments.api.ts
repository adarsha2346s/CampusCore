import { http } from '../../lib/api/http'
import type { EnrollmentResponse, PageResponse } from '../../types/api'

export interface EnrollmentCreateRequest {
  studentId: number
  courseId: number
  semester: string
  academicYear: string
}

export const enrollmentKeys = {
  all: ['admin', 'enrollments'] as const,
  detail: (id: number) => ['admin', 'enrollments', id] as const,
  mine: ['student', 'enrollments'] as const,
  page: (page: number, size: number) => ['admin', 'enrollments', 'page', page, size] as const,
}

export const getEnrollments = () => http<EnrollmentResponse[]>('enrollments')
export const getEnrollmentsPage = (page: number, size: number) => http<PageResponse<EnrollmentResponse>>('enrollments', { query: { page, size } })
export const getMyEnrollments = () => http<EnrollmentResponse[]>('enrollments/me')
export const getEnrollment = (id: number) => http<EnrollmentResponse>(`enrollments/${id}`)
export const createEnrollment = (request: EnrollmentCreateRequest) => http<EnrollmentResponse>('enrollments', { method: 'POST', query: { ...request } })
