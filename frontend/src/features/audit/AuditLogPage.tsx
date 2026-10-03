import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye, FileClock, RotateCcw } from 'lucide-react'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { PaginationControls } from '../../components/data-display/PaginationControls'
import { SelectField } from '../../components/forms/SelectField'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { getAuditLogsPage, auditKeys } from './audit.api'
import type { AuditFilter } from './audit.api'
import { formatDateTime } from '../../lib/format/datetime'
import type { AuditLogResponse } from '../../types/api'

function formatTimestamp(value: string) {
  return formatDateTime(value) ?? value
}

export function AuditLogPage() {
  const [search, setSearch] = useState('')
  const [userId, setUserId] = useState('ALL')
  const [entity, setEntity] = useState('ALL')
  const [action, setAction] = useState('ALL')
  const [page, setPage] = useState(0)
  const pageSize = 20
  const [viewing, setViewing] = useState<AuditLogResponse | null>(null)
  const activeFilter: AuditFilter = userId !== 'ALL' ? { kind: 'user', value: Number(userId) } : entity !== 'ALL' ? { kind: 'entity', value: entity } : action !== 'ALL' ? { kind: 'action', value: action } : { kind: 'all' }
  const source = useQuery({ queryKey: auditKeys.page(activeFilter, page, pageSize), queryFn: () => getAuditLogsPage(activeFilter, page, pageSize) })
  const options = useMemo(() => source.data?.content ?? [], [source.data?.content])
  const users = [...new Set(options.flatMap((log) => log.userId === null ? [] : [log.userId]))].sort((a, b) => a - b)
  const entities = [...new Set(options.map((log) => log.entityName))].sort()
  const actions = [...new Set(options.map((log) => log.action))].sort()
  const filtered = useMemo(() => options.filter((log) => {
    const matchesSearch = `${log.action} ${log.entityName} ${log.description ?? ''} ${log.userId ?? ''} ${log.entityId ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())
    return matchesSearch && (entity === 'ALL' || log.entityName === entity) && (action === 'ALL' || log.action === action)
  }), [options, search, entity, action])
  const clearFilters = () => { setUserId('ALL'); setEntity('ALL'); setAction('ALL'); setSearch(''); setPage(0) }
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
        <DirectoryToolbar search={search} onSearch={(value) => { setSearch(value); setPage(0) }} searchLabel="Search this page" countLabel={`${source.data?.totalElements ?? 0} entries total`} filters={(
          <><SelectField label="User" value={userId} onChange={(event) => { setUserId(event.target.value); setPage(0) }}><option value="ALL">All users</option>{users.map((id) => <option key={id} value={id}>User #{id}</option>)}</SelectField><SelectField label="Entity" value={entity} onChange={(event) => { setEntity(event.target.value); setPage(0) }}><option value="ALL">All entities</option>{entities.map((name) => <option key={name} value={name}>{name}</option>)}</SelectField><SelectField label="Action" value={action} onChange={(event) => { setAction(event.target.value); setPage(0) }}><option value="ALL">All actions</option>{actions.map((name) => <option key={name} value={name}>{name}</option>)}</SelectField><Button size="sm" variant="ghost" onClick={clearFilters} title="Clear search and filters"><RotateCcw size={15} aria-hidden="true" /> Clear</Button></>
        )} />
        {source.isPending ? <TableSkeleton rows={8} columns={4} label="Loading audit history" /> : source.isError ? <ErrorState error={source.error} onRetry={() => void source.refetch()} /> : !filtered.length ? <EmptyState title={source.data.totalElements ? 'No entries match this page search' : 'No audit entries available'} description={source.data.totalElements ? 'Try another search term or use the page controls.' : 'Recorded system activity will appear here when available.'} /> : (
          <ResourceTable caption="CampusCore audit log" columns={columns} rows={filtered} getRowKey={(log) => log.auditLogId} actions={(log) => <Button size="sm" variant="ghost" title="View audit entry details" aria-label={`View audit entry ${log.auditLogId}`} onClick={() => setViewing(log)}><Eye size={16} /></Button>} />
        )}
        {source.data && <PaginationControls page={source.data.page} size={source.data.size} totalElements={source.data.totalElements} totalPages={source.data.totalPages} onPageChange={setPage} />}
        <p className="directory-footnote">Search and filter options apply to the current page. Use pagination to browse the complete audit history.</p>
      </Card>
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`Audit entry #${viewing.auditLogId}`} description="Read-only event details" fields={[
        { label: 'Action', value: viewing.action }, { label: 'User ID', value: viewing.userId === null ? 'Not available' : viewing.userId },
        { label: 'Entity', value: viewing.entityName }, { label: 'Entity ID', value: viewing.entityId },
        { label: 'Recorded at', value: formatTimestamp(viewing.createdAt) }, { label: 'Description', value: viewing.description },
      ]} />}
    </div>
  )
}
