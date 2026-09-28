import { http } from '../../lib/api/http'
import type { EnrollmentResponse } from '../../types/api'

export interface EnrollmentCreateRequest {
  studentId: number
  courseId: number
  semester: string
  academicYear: string
}

export const enrollmentKeys = {
  all: ['admin', 'enrollments'] as const,
  detail: (id: number) => ['admin', 'enrollments', id] as const,
}

export const getEnrollments = () => http<EnrollmentResponse[]>('enrollments')
export const getEnrollment = (id: number) => http<EnrollmentResponse>(`enrollments/${id}`)
export const createEnrollment = (request: EnrollmentCreateRequest) => http<EnrollmentResponse>('enrollments', { method: 'POST', query: { ...request } })
