import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { PaginationControls } from '../../components/data-display/PaginationControls'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { notifyError } from '../admin-shared/feedback'
import { departmentKeys, getDepartments } from '../departments/departments.api'
import { getUsers, userKeys } from '../users/users.api'
import { FacultyFormDialog } from './FacultyFormDialog'
import { createFaculty, facultyKeys, getFacultyPage } from './faculty.api'
import type { FacultyResponse } from '../../types/api'

export function FacultyPage() {
  const client = useQueryClient()
  const [page, setPage] = useState(0)
  const pageSize = 20
  const faculty = useQuery({ queryKey: facultyKeys.page(page, pageSize), queryFn: () => getFacultyPage(page, pageSize) })
  const users = useQuery({ queryKey: userKeys.all, queryFn: getUsers })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [departmentId, setDepartmentId] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [viewing, setViewing] = useState<FacultyResponse | null>(null)

  const create = useMutation({ mutationFn: createFaculty, onSuccess: async () => { await client.invalidateQueries({ queryKey: ['admin'] }); setFormOpen(false); toast.success('Faculty profile created') } })
  const linkedUsers = new Set((faculty.data?.content ?? []).map((member) => member.userId))
  const availableUsers = (users.data ?? []).filter((user) => user.role === 'FACULTY' && user.active && !linkedUsers.has(user.userId))
  const userById = useMemo(() => new Map((users.data ?? []).map((user) => [user.userId, user])), [users.data])
  const departmentById = new Map((departments.data ?? []).map((department) => [department.departmentId, department]))
  const filtered = useMemo(() => (faculty.data?.content ?? []).filter((member) => {
    const user = userById.get(member.userId)
    const matchSearch = `${member.firstName} ${member.lastName ?? ''} ${member.employeeNumber} ${user?.username ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())
    return matchSearch && (status === 'ALL' || member.status === status) && (departmentId === 'ALL' || String(member.departmentId) === departmentId)
  }), [faculty.data, userById, search, status, departmentId])

  async function save(request: Parameters<typeof createFaculty>[0]) {
    try { await create.mutateAsync(request) } catch (error) { notifyError(error, 'The faculty profile could not be created.') }
  }

  const columns = [
    { key: 'faculty', header: 'Faculty member', render: (member: FacultyResponse) => <div className="identity-cell"><span className="identity-avatar identity-avatar--violet" aria-hidden="true">{member.firstName.slice(0, 1).toUpperCase()}</span><span><strong>{member.firstName} {member.lastName}</strong><small>{userById.get(member.userId)?.username ?? `User #${member.userId}`}</small></span></div> },
    { key: 'employee', header: 'Employee no.', render: (member: FacultyResponse) => <span className="mono-label">{member.employeeNumber}</span> },
    { key: 'department', header: 'Department', render: (member: FacultyResponse) => departmentById.get(member.departmentId)?.name ?? `Department #${member.departmentId}` },
    { key: 'phone', header: 'Phone', mobileHidden: true, render: (member: FacultyResponse) => member.phone || <span className="muted">—</span> },
    { key: 'status', header: 'Status', render: (member: FacultyResponse) => <StatusBadge status={member.status} /> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic directory" title="Faculty" description="Browse faculty profiles and create profiles for existing faculty accounts." action={<Button onClick={() => setFormOpen(true)} disabled={users.isError || users.isPending || departments.isError || departments.isPending || departments.data?.length === 0 || availableUsers.length === 0}><Plus size={17} aria-hidden="true" /> Add faculty</Button>} />
      {(users.isError || departments.isError) && <ErrorState error={users.error ?? departments.error} onRetry={() => { void users.refetch(); void departments.refetch() }} />}
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={(value) => { setSearch(value); setPage(0) }} searchLabel="Search this page" countLabel={`${filtered.length} shown · ${faculty.data?.totalElements ?? 0} total`} filters={(
          <><SelectField label="Status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></SelectField><SelectField label="Department" value={departmentId} onChange={(event) => { setDepartmentId(event.target.value); setPage(0) }}><option value="ALL">All departments</option>{departments.data?.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.code}</option>)}</SelectField></>
        )} />
        {faculty.isPending ? <TableSkeleton rows={8} columns={5} label="Loading faculty records" /> : faculty.isError ? <ErrorState error={faculty.error} onRetry={() => void faculty.refetch()} /> : filtered.length === 0 ? <EmptyState title={faculty.data.totalElements === 0 ? 'No faculty profiles yet' : 'No faculty on this page match these filters'} description={faculty.data.totalElements === 0 ? 'Create an active FACULTY user account, then add a faculty profile.' : 'Try another search, filter or page.'} /> : (
          <ResourceTable caption="Faculty directory" columns={columns} rows={filtered} getRowKey={(member) => member.facultyId} actions={(member) => <Button size="sm" variant="ghost" aria-label={`View ${member.firstName} ${member.lastName ?? ''}`} title="View faculty details" onClick={() => setViewing(member)}><Eye size={16} /></Button>} />
        )}
        {!faculty.isPending && !faculty.isError && faculty.data && <PaginationControls page={faculty.data.page} size={faculty.data.size} totalElements={faculty.data.totalElements} totalPages={faculty.data.totalPages} onPageChange={setPage} />}
      </Card>
      <FacultyFormDialog open={formOpen} onOpenChange={setFormOpen} users={availableUsers} departments={departments.data ?? []} onSubmit={save} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`${viewing.firstName} ${viewing.lastName ?? ''}`} description="Faculty profile details" fields={[
        { label: 'Faculty ID', value: viewing.facultyId }, { label: 'Employee number', value: viewing.employeeNumber },
        { label: 'Account', value: userById.get(viewing.userId)?.username ?? `User #${viewing.userId}` },
        { label: 'Department', value: departmentById.get(viewing.departmentId)?.name ?? `Department #${viewing.departmentId}` },
        { label: 'Phone', value: viewing.phone }, { label: 'Status', value: viewing.status },
      ]} />}
    </div>
  )
}
