import { http } from '../../lib/api/http'
import type { StudentDashboardResponse } from '../../types/api'

export const studentDashboardKeys = {
  detail: (studentId: number) => ['student', 'dashboard', studentId] as const,
}

export const getStudentDashboard = (studentId: number) =>
  http<StudentDashboardResponse>(`dashboard/student/${studentId}`)
