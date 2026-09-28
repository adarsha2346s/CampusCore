import { http } from '../../lib/api/http'
import type { Department, DepartmentRequest } from '../../types/api'

export const departmentKeys = {
  all: ['admin', 'departments'] as const,
  detail: (id: number) => ['admin', 'departments', id] as const,
}

export const getDepartments = () => http<Department[]>('departments')
export const getDepartment = (id: number) => http<Department>(`departments/${id}`)
export const createDepartment = (request: DepartmentRequest) => http<Department>('departments', { method: 'POST', body: request })
export const updateDepartment = (id: number, request: DepartmentRequest) => http<Department>(`departments/${id}`, { method: 'PUT', body: request })
export const deleteDepartment = (id: number) => http<void>(`departments/${id}`, { method: 'DELETE' })
