import { http } from '../../lib/api/http'
import type { AssessmentResponse, AssessmentType, PageResponse } from '../../types/api'

export interface AssessmentCreateRequest {
  courseId: number
  name: string
  assessmentType: AssessmentType
  maxMarks: string
  weight: string
}

export const assessmentKeys = {
  all: ['admin', 'assessments'] as const,
  byCourse: (courseId: string) => ['admin', 'assessments', 'course', courseId] as const,
  detail: (id: number) => ['admin', 'assessments', id] as const,
  page: (page: number, size: number, courseId?: number) => ['admin', 'assessments', 'page', page, size, courseId ?? null] as const,
}

export const getAssessments = () => http<AssessmentResponse[]>('assessments')
export const getAssessmentsPage = (page: number, size: number) => http<PageResponse<AssessmentResponse>>('assessments', { query: { page, size } })
export const getAssessment = (id: number) => http<AssessmentResponse>(`assessments/${id}`)
export const getAssessmentsByCourse = (courseId: number) => http<AssessmentResponse[]>(`assessments/course/${courseId}`)
export const getAssessmentsByCoursePage = (courseId: number, page: number, size: number) => http<PageResponse<AssessmentResponse>>(`assessments/course/${courseId}`, { query: { page, size } })
export const createAssessment = (request: AssessmentCreateRequest) => http<AssessmentResponse>('assessments', { method: 'POST', query: { ...request } })

/**
 * Payload for changing an existing assessment.
 *
 * The course is absent by design: it owns the assessment and any marks recorded
 * against it, so it cannot be reassigned from here.
 */
export interface AssessmentUpdateRequest {
  name: string
  assessmentType: AssessmentType
  maxMarks: number
  weight: number
  assessmentDate: string | null
}

/**
 * Update an assessment.
 *
 * Name, type, weight and date stay editable at any time. The server rejects a
 * changed maximum marks once marks reference the assessment, because rescaling it
 * would reinterpret every grade already recorded against it.
 */
export const updateAssessment = (id: number, request: AssessmentUpdateRequest) =>
  http<AssessmentResponse>(`assessments/${id}`, { method: 'PUT', body: request })

/**
 * Delete an assessment.
 *
 * Rejected with 409 while marks reference it, since the assessment is the parent
 * of those mark rows.
 */
export const deleteAssessment = (id: number) =>
  http<void>(`assessments/${id}`, { method: 'DELETE' })

/**
 * Assessment metadata for the calling student's own marks.
 *
 * Backed by GET /api/v1/assessments/me, which is student-only and takes no
 * identifier: the server resolves ownership from the session. The general
 * assessment routes remain administrator/faculty only and are deliberately not
 * used from the student workspace.
 */
export interface AssessmentSelfResponse {
  assessmentId: number
  name: string
  assessmentType: string
  maxMarks: string
}

export const studentAssessmentKeys = {
  mine: ['student', 'assessments', 'me'] as const,
}

export const getMyAssessments = () => http<AssessmentSelfResponse[]>('assessments/me')
