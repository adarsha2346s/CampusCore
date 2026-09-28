import { http } from '../../lib/api/http'
import type { AuditLogResponse } from '../../types/api'

export type AuditFilter = { kind: 'all' } | { kind: 'user'; value: number } | { kind: 'entity'; value: string } | { kind: 'action'; value: string }

export const auditKeys = {
  all: ['admin', 'audit-logs'] as const,
  filtered: (filter: AuditFilter) => ['admin', 'audit-logs', filter.kind, 'value' in filter ? filter.value : ''] as const,
}

export const getAuditLogs = () => http<AuditLogResponse[]>('audit-logs')
export const getAuditLogsByUser = (userId: number) => http<AuditLogResponse[]>(`audit-logs/user/${userId}`)
export const getAuditLogsByEntity = (entityName: string) => http<AuditLogResponse[]>(`audit-logs/entity/${encodeURIComponent(entityName)}`)
export const getAuditLogsByAction = (action: string) => http<AuditLogResponse[]>(`audit-logs/action/${encodeURIComponent(action)}`)
