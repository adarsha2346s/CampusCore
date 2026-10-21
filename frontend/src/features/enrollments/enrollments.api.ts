import { http } from '../../lib/api/http'
import type { EnrollmentResponse, EnrollmentStatus, PageResponse } from '../../types/api'

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

/**
 * Enrollments of a single course.
 *
 * Backed by GET /api/v1/enrollments/course/{courseId}, which the API answers for
 * an administrator or a faculty member currently assigned to that course.
 */
export const getEnrollmentsByCourse = (courseId: number) =>
  http<EnrollmentResponse[]>(`enrollments/course/${courseId}`)

/** Payload for changing an enrollment's term or lifecycle status. */
export interface EnrollmentUpdateRequest {
  semester: string
  academicYear: string
  status: EnrollmentStatus
}

/**
 * Update an enrollment.
 *
 * Status is the lifecycle control and can always be changed. The term is rejected
 * by the server once the enrollment carries marks or attendance, because
 * re-terms graded work would silently move those results.
 */
export const updateEnrollment = (id: number, request: EnrollmentUpdateRequest) =>
  http<EnrollmentResponse>(`enrollments/${id}`, { method: 'PUT', body: request })

/**
 * Delete an enrollment.
 *
 * Rejected with 409 when the enrollment already has marks or attendance records;
 * change the status instead to record the outcome.
 */
export const deleteEnrollment = (id: number) =>
  http<void>(`enrollments/${id}`, { method: 'DELETE' })
