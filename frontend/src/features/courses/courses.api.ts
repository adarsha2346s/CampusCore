import { http } from '../../lib/api/http'
import type { CourseRequest, CourseResponse } from '../../types/api'

export const courseKeys = {
  all: ['admin', 'courses'] as const,
  detail: (id: number) => ['admin', 'courses', id] as const,
}

export const getCourses = () => http<CourseResponse[]>('courses')
export const getCourse = (id: number) => http<CourseResponse>(`courses/${id}`)
export const createCourse = (request: CourseRequest) => http<CourseResponse>('courses', { method: 'POST', body: request })
export const updateCourse = (id: number, request: CourseRequest) => http<CourseResponse>(`courses/${id}`, { method: 'PUT', body: request })
export const deleteCourse = (id: number) => http<void>(`courses/${id}`, { method: 'DELETE' })
