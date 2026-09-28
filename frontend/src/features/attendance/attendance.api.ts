import { http } from '../../lib/api/http'
import type { AttendanceRecordResponse, AttendanceSessionResponse, AttendanceStatus } from '../../types/api'

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
}

export const attendanceRecordKeys = {
  all: ['admin', 'attendance-records'] as const,
  bySession: (id: string) => ['admin', 'attendance-records', 'session', id] as const,
  byEnrollment: (id: string) => ['admin', 'attendance-records', 'enrollment', id] as const,
  detail: (id: number) => ['admin', 'attendance-records', id] as const,
}

export const getAttendanceSessions = () => http<AttendanceSessionResponse[]>('attendance-sessions')
export const getAttendanceSession = (id: number) => http<AttendanceSessionResponse>(`attendance-sessions/${id}`)
export const getAttendanceSessionsByCourse = (id: number) => http<AttendanceSessionResponse[]>(`attendance-sessions/course/${id}`)
export const getAttendanceSessionsByFaculty = (id: number) => http<AttendanceSessionResponse[]>(`attendance-sessions/faculty/${id}`)
export const createAttendanceSession = (request: AttendanceSessionCreateRequest) => http<AttendanceSessionResponse>('attendance-sessions', { method: 'POST', query: { ...request } })

export const getAttendanceRecords = () => http<AttendanceRecordResponse[]>('attendance-records')
export const getAttendanceRecord = (id: number) => http<AttendanceRecordResponse>(`attendance-records/${id}`)
export const getAttendanceRecordsBySession = (id: number) => http<AttendanceRecordResponse[]>(`attendance-records/session/${id}`)
export const getAttendanceRecordsByEnrollment = (id: number) => http<AttendanceRecordResponse[]>(`attendance-records/enrollment/${id}`)
export const createAttendanceRecord = (request: AttendanceRecordCreateRequest) => http<AttendanceRecordResponse>('attendance-records', { method: 'POST', query: { ...request } })
