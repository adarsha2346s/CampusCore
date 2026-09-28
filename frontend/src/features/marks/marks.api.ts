import { http } from '../../lib/api/http'
import type { MarkResponse } from '../../types/api'

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
}

export const getMarks = () => http<MarkResponse[]>('marks')
export const getMark = (id: number) => http<MarkResponse>(`marks/${id}`)
export const getMarksByEnrollment = (id: number) => http<MarkResponse[]>(`marks/enrollment/${id}`)
export const getMarksByAssessment = (id: number) => http<MarkResponse[]>(`marks/assessment/${id}`)
export const createMark = (request: MarkCreateRequest) => http<MarkResponse>('marks', { method: 'POST', query: { ...request } })
