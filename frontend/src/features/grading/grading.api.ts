import { http } from '../../lib/api/http'
import type { GradingPolicy, GradingPolicyRequest, GpaResult } from '../../types/api'

export const gradingPolicyKeys = { all: ['admin', 'grading-policies'] as const }
export const gpaKeys = { enrollment: (id: string, policy: string) => ['gpa', 'enrollment', id, policy] as const }

export const getGradingPolicies = () => http<GradingPolicy[]>('grading-policies')
export const createGradingPolicy = (request: GradingPolicyRequest) => http<GradingPolicy>('grading-policies', { method: 'POST', body: request })
export const getEnrollmentGpa = (enrollmentId: number, policyName?: string) => http<GpaResult>(`gpa/enrollment/${enrollmentId}`, { query: { policyName } })
