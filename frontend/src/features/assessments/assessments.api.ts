import { http } from '../../lib/api/http'
import type { AssessmentResponse, AssessmentType } from '../../types/api'

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
}

export const getAssessments = () => http<AssessmentResponse[]>('assessments')
export const getAssessment = (id: number) => http<AssessmentResponse>(`assessments/${id}`)
export const getAssessmentsByCourse = (courseId: number) => http<AssessmentResponse[]>(`assessments/course/${courseId}`)
export const createAssessment = (request: AssessmentCreateRequest) => http<AssessmentResponse>('assessments', { method: 'POST', query: { ...request } })
