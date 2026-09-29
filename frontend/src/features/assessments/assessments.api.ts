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
