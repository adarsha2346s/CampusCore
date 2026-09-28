import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye, FileClock, RotateCcw } from 'lucide-react'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { SelectField } from '../../components/forms/SelectField'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { getAuditLogs, getAuditLogsByAction, getAuditLogsByEntity, getAuditLogsByUser, auditKeys } from './audit.api'
import type { AuditFilter } from './audit.api'
import type { AuditLogResponse } from '../../types/api'

function formatTimestamp(value: string) {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(parsed)
}

export function AuditLogPage() {
  const [search, setSearch] = useState('')
  const [userId, setUserId] = useState('ALL')
  const [entity, setEntity] = useState('ALL')
  const [action, setAction] = useState('ALL')
  const [viewing, setViewing] = useState<AuditLogResponse | null>(null)
  const allLogs = useQuery({ queryKey: auditKeys.all, queryFn: getAuditLogs })
  const activeFilter: AuditFilter = userId !== 'ALL' ? { kind: 'user', value: Number(userId) } : entity !== 'ALL' ? { kind: 'entity', value: entity } : action !== 'ALL' ? { kind: 'action', value: action } : { kind: 'all' }
  const filteredLogs = useQuery({
    queryKey: auditKeys.filtered(activeFilter),
    queryFn: () => activeFilter.kind === 'user' ? getAuditLogsByUser(activeFilter.value) : activeFilter.kind === 'entity' ? getAuditLogsByEntity(activeFilter.value) : activeFilter.kind === 'action' ? getAuditLogsByAction(activeFilter.value) : getAuditLogs(),
    enabled: activeFilter.kind !== 'all',
  })
  const source = activeFilter.kind === 'all' ? allLogs : filteredLogs
  const options = allLogs.data ?? []
  const users = [...new Set(options.flatMap((log) => log.userId === null ? [] : [log.userId]))].sort((a, b) => a - b)
  const entities = [...new Set(options.map((log) => log.entityName))].sort()
  const actions = [...new Set(options.map((log) => log.action))].sort()
  const filtered = useMemo(() => (source.data ?? []).filter((log) => {
    const matchesSearch = `${log.action} ${log.entityName} ${log.description ?? ''} ${log.userId ?? ''} ${log.entityId ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())
    return matchesSearch && (entity === 'ALL' || log.entityName === entity) && (action === 'ALL' || log.action === action)
  }), [source.data, search, entity, action])
  const clearFilters = () => { setUserId('ALL'); setEntity('ALL'); setAction('ALL'); setSearch('') }
  const columns = [
    { key: 'action', header: 'Activity', render: (log: AuditLogResponse) => <div className="identity-cell"><span className="entity-icon"><FileClock size={17} aria-hidden="true" /></span><span><strong>{log.action}</strong><small>{log.description || 'No description provided'}</small></span></div> },
    { key: 'user', header: 'User', render: (log: AuditLogResponse) => log.userId === null ? <span className="muted">Unavailable</span> : `User #${log.userId}` },
    { key: 'entity', header: 'Entity', render: (log: AuditLogResponse) => <span><Badge className="role-badge">{log.entityName}</Badge>{log.entityId !== null && <small className="cell-subtext">ID #{log.entityId}</small>}</span> },
    { key: 'time', header: 'Recorded', mobileHidden: true, render: (log: AuditLogResponse) => <time dateTime={log.createdAt}>{formatTimestamp(log.createdAt)}</time> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Governance & accountability" title="Audit log" description="Read-only history of recorded account and academic operations." />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search audit log" countLabel={`${filtered.length} matching entries`} filters={(
          <><SelectField label="User" value={userId} onChange={(event) => setUserId(event.target.value)}><option value="ALL">All users</option>{users.map((id) => <option key={id} value={id}>User #{id}</option>)}</SelectField><SelectField label="Entity" value={entity} onChange={(event) => setEntity(event.target.value)}><option value="ALL">All entities</option>{entities.map((name) => <option key={name} value={name}>{name}</option>)}</SelectField><SelectField label="Action" value={action} onChange={(event) => setAction(event.target.value)}><option value="ALL">All actions</option>{actions.map((name) => <option key={name} value={name}>{name}</option>)}</SelectField><Button size="sm" variant="ghost" onClick={clearFilters} title="Clear search and filters"><RotateCcw size={15} aria-hidden="true" /> Clear</Button></>
        )} />
        {allLogs.isPending || source.isPending ? <LoadingState label="Loading audit history" /> : source.isError ? <ErrorState error={source.error} onRetry={() => { void allLogs.refetch(); void filteredLogs.refetch() }} /> : !filtered.length ? <EmptyState title={options.length ? 'No audit entries match these filters' : 'No audit entries available'} description={options.length ? 'Try another user, entity, action or search term.' : 'Recorded system activity will appear here when available.'} /> : (
          <ResourceTable caption="CampusCore audit log" columns={columns} rows={filtered} getRowKey={(log) => log.auditLogId} actions={(log) => <Button size="sm" variant="ghost" title="View audit entry details" aria-label={`View audit entry ${log.auditLogId}`} onClick={() => setViewing(log)}><Eye size={16} /></Button>} />
        )}
        <p className="directory-footnote">Showing the complete response for this filter. The audit API does not provide server pagination.</p>
      </Card>
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`Audit entry #${viewing.auditLogId}`} description="Read-only event details" fields={[
        { label: 'Action', value: viewing.action }, { label: 'User ID', value: viewing.userId === null ? 'Not available' : viewing.userId },
        { label: 'Entity', value: viewing.entityName }, { label: 'Entity ID', value: viewing.entityId },
        { label: 'Recorded at', value: formatTimestamp(viewing.createdAt) }, { label: 'Description', value: viewing.description },
      ]} />}
    </div>
  )
}
