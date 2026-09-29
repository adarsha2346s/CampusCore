import { http } from '../../lib/api/http'
import type { AttendanceRecordResponse, AttendanceSessionResponse, AttendanceStatus, PageResponse } from '../../types/api'

export interface AttendanceSessionCreateRequest {
  courseId: number
  facultyId: number
  sessionDate: string
  topic: string
}

export interface AttendanceRecordCreateRequest {
  attendanceSessionId: number
  enrollmentId: number
  status: AttendanceStatus
}

export const attendanceSessionKeys = {
  all: ['admin', 'attendance-sessions'] as const,
  byCourse: (id: string) => ['admin', 'attendance-sessions', 'course', id] as const,
  byFaculty: (id: string) => ['admin', 'attendance-sessions', 'faculty', id] as const,
  detail: (id: number) => ['admin', 'attendance-sessions', id] as const,
  page: (page: number, size: number) => ['admin', 'attendance-sessions', 'page', page, size] as const,
  filteredPage: (filter: string, id: string, page: number, size: number) => ['admin', 'attendance-sessions', filter, id, 'page', page, size] as const,
}

export const attendanceRecordKeys = {
  all: ['admin', 'attendance-records'] as const,
  bySession: (id: string) => ['admin', 'attendance-records', 'session', id] as const,
  byEnrollment: (id: string) => ['admin', 'attendance-records', 'enrollment', id] as const,
  detail: (id: number) => ['admin', 'attendance-records', id] as const,
  page: (page: number, size: number) => ['admin', 'attendance-records', 'page', page, size] as const,
  filteredPage: (filter: string, id: string, page: number, size: number) => ['admin', 'attendance-records', filter, id, 'page', page, size] as const,
}

export const getAttendanceSessions = () => http<AttendanceSessionResponse[]>('attendance-sessions')
export const getAttendanceSessionsPage = (page: number, size: number) => http<PageResponse<AttendanceSessionResponse>>('attendance-sessions', { query: { page, size } })
export const getAttendanceSession = (id: number) => http<AttendanceSessionResponse>(`attendance-sessions/${id}`)
export const getAttendanceSessionsByCourse = (id: number) => http<AttendanceSessionResponse[]>(`attendance-sessions/course/${id}`)
export const getAttendanceSessionsByFaculty = (id: number) => http<AttendanceSessionResponse[]>(`attendance-sessions/faculty/${id}`)
export const getAttendanceSessionsByCoursePage = (id: number, page: number, size: number) => http<PageResponse<AttendanceSessionResponse>>(`attendance-sessions/course/${id}`, { query: { page, size } })
export const getAttendanceSessionsByFacultyPage = (id: number, page: number, size: number) => http<PageResponse<AttendanceSessionResponse>>(`attendance-sessions/faculty/${id}`, { query: { page, size } })
export const createAttendanceSession = (request: AttendanceSessionCreateRequest) => http<AttendanceSessionResponse>('attendance-sessions', { method: 'POST', query: { ...request } })

export const getAttendanceRecords = () => http<AttendanceRecordResponse[]>('attendance-records')
export const getAttendanceRecordsPage = (page: number, size: number) => http<PageResponse<AttendanceRecordResponse>>('attendance-records', { query: { page, size } })
export const getAttendanceRecord = (id: number) => http<AttendanceRecordResponse>(`attendance-records/${id}`)
export const getAttendanceRecordsBySession = (id: number) => http<AttendanceRecordResponse[]>(`attendance-records/session/${id}`)
export const getAttendanceRecordsByEnrollment = (id: number) => http<AttendanceRecordResponse[]>(`attendance-records/enrollment/${id}`)
export const getAttendanceRecordsBySessionPage = (id: number, page: number, size: number) => http<PageResponse<AttendanceRecordResponse>>(`attendance-records/session/${id}`, { query: { page, size } })
export const getAttendanceRecordsByEnrollmentPage = (id: number, page: number, size: number) => http<PageResponse<AttendanceRecordResponse>>(`attendance-records/enrollment/${id}`, { query: { page, size } })
export const createAttendanceRecord = (request: AttendanceRecordCreateRequest) => http<AttendanceRecordResponse>('attendance-records', { method: 'POST', query: { ...request } })
