import { http } from '../../lib/api/http'
import type { StudentRequest, StudentResponse } from '../../types/api'

export const studentKeys = {
  all: ['admin', 'students'] as const,
  detail: (id: number) => ['admin', 'students', id] as const,
}

export const getStudents = () => http<StudentResponse[]>('students')
export const getStudent = (id: number) => http<StudentResponse>(`students/${id}`)
export const createStudent = (request: StudentRequest) => http<StudentResponse>('students', { method: 'POST', body: request })
export const updateStudent = (id: number, request: StudentRequest) => http<StudentResponse>(`students/${id}`, { method: 'PUT', body: request })
export const deactivateStudent = (id: number) => http<void>(`students/${id}`, { method: 'DELETE' })
