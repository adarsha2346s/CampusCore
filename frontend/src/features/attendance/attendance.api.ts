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

/**
 * Payload for changing an attendance session.
 *
 * Course and faculty are omitted by design: attendance records are filed against
 * the session, so the server keeps the offering and lecturer it was taken under.
 */
export interface AttendanceSessionUpdateRequest {
  sessionDate: string
  topic: string
}

/**
 * Update a session's date and topic.
 *
 * Allowed even when attendance has been recorded: correcting a mistyped date does
 * not change what was recorded.
 */
export const updateAttendanceSession = (id: number, request: AttendanceSessionUpdateRequest) =>
  http<AttendanceSessionResponse>(`attendance-sessions/${id}`, { method: 'PUT', body: request })

/**
 * Delete an attendance session.
 *
 * Rejected with 409 while attendance records exist for it, since those rows are
 * the child records of the session.
 */
export const deleteAttendanceSession = (id: number) =>
  http<void>(`attendance-sessions/${id}`, { method: 'DELETE' })

/** The signed-in student's own attendance, with course and session already joined. */
export interface StudentAttendance {
  attendanceRecordId: number
  attendanceSessionId: number
  courseCode: string
  courseName: string
  sessionDate: string
  topic: string | null
  status: string
  markedAt: string | null
}

export const studentAttendanceKeys = {
  mine: ['student', 'attendance-records', 'me'] as const,
}

/**
 * The caller's own attendance.
 *
 * Backed by GET /api/v1/attendance-records/me, which is student-only and takes no
 * identifier. The workspace previously called the institution-wide session listing
 * at GET /api/v1/attendance-sessions, which is administrator-only, so a student
 * asking for their own attendance was refused outright.
 */
export const getMyAttendance = () => http<StudentAttendance[]>('attendance-records/me')
