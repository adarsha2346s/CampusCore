import { http } from '../../lib/api/http'
import type { CourseRequest, CourseResponse, PageResponse } from '../../types/api'

export const courseKeys = {
  all: ['admin', 'courses'] as const,
  detail: (id: number) => ['admin', 'courses', id] as const,
  page: (page: number, size: number) => ['admin', 'courses', 'page', page, size] as const,
}

export const getCourses = () => http<CourseResponse[]>('courses')
export const getCoursesPage = (page: number, size: number) => http<PageResponse<CourseResponse>>('courses', { query: { page, size } })
export const getCourse = (id: number) => http<CourseResponse>(`courses/${id}`)
export const createCourse = (request: CourseRequest) => http<CourseResponse>('courses', { method: 'POST', body: request })
export const updateCourse = (id: number, request: CourseRequest) => http<CourseResponse>(`courses/${id}`, { method: 'PUT', body: request })
export const deleteCourse = (id: number) => http<void>(`courses/${id}`, { method: 'DELETE' })

/**
 * Retire a course from the catalog.
 *
 * The status change goes through its own endpoint: the course update payload
 * carries no status, and copying one would silently resurrect a deactivated
 * course. There is no matching reactivate route, so this is one way.
 */
export const deactivateCourse = (id: number) =>
  http<CourseResponse>(`courses/${id}/deactivate`, { method: 'POST' })
