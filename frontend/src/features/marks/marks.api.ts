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
