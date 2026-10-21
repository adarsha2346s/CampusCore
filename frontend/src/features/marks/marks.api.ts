import { http } from '../../lib/api/http'
import type { MarkResponse, PageResponse } from '../../types/api'

export interface MarkCreateRequest {
  enrollmentId: number
  assessmentId: number
  marksObtained: string
}

export const markKeys = {
  all: ['admin', 'marks'] as const,
  byEnrollment: (id: string) => ['admin', 'marks', 'enrollment', id] as const,
  byAssessment: (id: string) => ['admin', 'marks', 'assessment', id] as const,
  detail: (id: number) => ['admin', 'marks', id] as const,
  page: (page: number, size: number) => ['admin', 'marks', 'page', page, size] as const,
  filteredPage: (page: number, size: number, enrollmentId: string, assessmentId: string) => ['admin', 'marks', 'page', page, size, enrollmentId, assessmentId] as const,
}

export const getMarks = () => http<MarkResponse[]>('marks')
export const getMarksPage = (page: number, size: number) => http<PageResponse<MarkResponse>>('marks', { query: { page, size } })
export const getMark = (id: number) => http<MarkResponse>(`marks/${id}`)
export const getMarksByEnrollment = (id: number) => http<MarkResponse[]>(`marks/enrollment/${id}`)
export const getMarksByAssessment = (id: number) => http<MarkResponse[]>(`marks/assessment/${id}`)
export const getMarksByEnrollmentPage = (id: number, page: number, size: number) => http<PageResponse<MarkResponse>>(`marks/enrollment/${id}`, { query: { page, size } })
export const getMarksByAssessmentPage = (id: number, page: number, size: number) => http<PageResponse<MarkResponse>>(`marks/assessment/${id}`, { query: { page, size } })
export const createMark = (request: MarkCreateRequest) => http<MarkResponse>('marks', { method: 'POST', query: { ...request } })

/**
 * Payload for correcting a recorded mark.
 *
 * Only the awarded value travels. The enrollment and assessment are omitted by
 * design: they identify whose result this is and what it measures, and the
 * server keeps them as stored.
 */
export interface MarkUpdateRequest {
  marksObtained: number
}

/**
 * Update a recorded mark.
 *
 * Rejected with 400 when the value is negative or over-precise, and with 409 when
 * it would exceed the assessment's maximum.
 */
export const updateMark = (id: number, request: MarkUpdateRequest) =>
  http<MarkResponse>(`marks/${id}`, { method: 'PUT', body: request })

/**
 * Delete a recorded mark.
 *
 * A mark is a leaf record, so this is allowed; the server still re-checks
 * enrollment scope and reports 404 for an unknown mark.
 */
export const deleteMark = (id: number) => http<void>(`marks/${id}`, { method: 'DELETE' })

/** The signed-in student's own marks, with course and assessment already joined. */
export interface StudentMark {
  markId: number
  courseCode: string
  courseName: string
  semester: string
  academicYear: string
  assessmentName: string
  assessmentType: string
  marksObtained: string
  maxMarks: string
  weight: string
  enteredAt: string | null
}

export const studentMarkKeys = {
  mine: ['student', 'marks', 'me'] as const,
}

/**
 * The caller's own marks.
 *
 * Backed by GET /api/v1/marks/me, which is student-only and takes no identifier:
 * the server resolves ownership from the session. This replaces a fan-out of one
 * request per enrollment plus separate course and assessment lookups, and it takes
 * no identifier so it cannot be pointed at another student.
 */
export const getMyMarks = () => http<StudentMark[]>('marks/me')
