import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Pencil, Plus, UserX } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { PaginationControls } from '../../components/data-display/PaginationControls'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { notifyError } from '../admin-shared/feedback'
import { getDepartments, departmentKeys } from '../departments/departments.api'
import { getUsers, userKeys } from '../users/users.api'
import { StudentFormDialog } from './StudentFormDialog'
import { createStudent, deactivateStudent, getStudentsPage, studentKeys, updateStudent } from './students.api'
import type { StudentRequest, StudentResponse } from '../../types/api'

export function StudentsPage() {
  const client = useQueryClient()
  const [page, setPage] = useState(0)
  const pageSize = 20
  const students = useQuery({ queryKey: studentKeys.page(page, pageSize), queryFn: () => getStudentsPage(page, pageSize) })
  const users = useQuery({ queryKey: userKeys.all, queryFn: getUsers })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [departmentId, setDepartmentId] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<StudentResponse | null>(null)
  const [viewing, setViewing] = useState<StudentResponse | null>(null)

  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  const create = useMutation({ mutationFn: createStudent, onSuccess: async () => { await refresh(); setFormOpen(false); setEditing(null); toast.success('Student profile created') } })
  const update = useMutation({ mutationFn: ({ id, request }: { id: number; request: StudentRequest }) => updateStudent(id, request), onSuccess: async () => { await refresh(); setFormOpen(false); setEditing(null); toast.success('Student profile updated') } })
  const deactivate = useMutation({ mutationFn: deactivateStudent, onSuccess: async () => { await refresh(); toast.success('Student profile deactivated') } })

  const linkedUserIds = new Set((students.data?.content ?? []).filter((student) => student.studentId !== editing?.studentId).map((student) => student.userId))
  const availableUsers = (users.data ?? []).filter((user) => user.role === 'STUDENT' && user.active && !linkedUserIds.has(user.userId))
  const userById = useMemo(() => new Map((users.data ?? []).map((user) => [user.userId, user])), [users.data])
  const departmentById = new Map((departments.data ?? []).map((department) => [department.departmentId, department]))
  const filtered = useMemo(() => (students.data?.content ?? []).filter((student) => {
    const user = userById.get(student.userId)
    const matchSearch = `${student.firstName} ${student.lastName ?? ''} ${student.enrollmentNumber} ${user?.username ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())
    return matchSearch && (status === 'ALL' || student.status === status) && (departmentId === 'ALL' || String(student.departmentId) === departmentId)
  }), [students.data, userById, search, status, departmentId])

  async function save(request: StudentRequest) {
    try {
      if (editing) await update.mutateAsync({ id: editing.studentId, request })
      else await create.mutateAsync(request)
    } catch (error) { notifyError(error, 'The student profile could not be saved.') }
  }

  const columns = [
    { key: 'student', header: 'Student', render: (student: StudentResponse) => <div className="identity-cell"><span className="identity-avatar identity-avatar--teal" aria-hidden="true">{student.firstName.slice(0, 1).toUpperCase()}</span><span><strong>{student.firstName} {student.lastName}</strong><small>{userById.get(student.userId)?.username ?? `User #${student.userId}`}</small></span></div> },
    { key: 'enrollment', header: 'Enrollment no.', render: (student: StudentResponse) => <span className="mono-label">{student.enrollmentNumber}</span> },
    { key: 'department', header: 'Department', render: (student: StudentResponse) => departmentById.get(student.departmentId)?.name ?? `Department #${student.departmentId}` },
    { key: 'year', header: 'Admission year', mobileHidden: true, render: (student: StudentResponse) => student.admissionYear },
    { key: 'status', header: 'Status', render: (student: StudentResponse) => <StatusBadge status={student.status} /> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic directory" title="Students" description="Maintain student profiles, enrollment identity and department placement." action={<Button onClick={() => { setEditing(null); setFormOpen(true) }} disabled={users.isError || users.isPending || departments.isError || departments.isPending || departments.data?.length === 0 || availableUsers.length === 0}><Plus size={17} aria-hidden="true" /> Add student</Button>} />
      {(users.isError || departments.isError) && <ErrorState error={users.error ?? departments.error} onRetry={() => { void users.refetch(); void departments.refetch() }} />}
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={(value) => { setSearch(value); setPage(0) }} searchLabel="Search this page" countLabel={`${filtered.length} shown · ${students.data?.totalElements ?? 0} total`} filters={(
          <><SelectField label="Status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="GRADUATED">Graduated</option></SelectField><SelectField label="Department" value={departmentId} onChange={(event) => { setDepartmentId(event.target.value); setPage(0) }}><option value="ALL">All departments</option>{departments.data?.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.code}</option>)}</SelectField></>
        )} />
        {students.isPending ? <LoadingState label="Loading student records" /> : students.isError ? <ErrorState error={students.error} onRetry={() => void students.refetch()} /> : filtered.length === 0 ? <EmptyState title={students.data.totalElements === 0 ? 'No student profiles yet' : 'No students on this page match these filters'} description={students.data.totalElements === 0 ? 'Create or select a user account with the STUDENT role, then add a profile.' : 'Try another name, department or move to another page.'} /> : (
          <ResourceTable caption="Student directory" columns={columns} rows={filtered} getRowKey={(student) => student.studentId} actions={(student) => (
            <div className="row-actions">
              <Button size="sm" variant="ghost" aria-label={`View ${student.firstName} ${student.lastName ?? ''}`} title="View details" onClick={() => setViewing(student)}><Eye size={16} /></Button>
              <Button size="sm" variant="ghost" aria-label={`Edit ${student.firstName} ${student.lastName ?? ''}`} title="Edit profile" onClick={() => { setEditing(student); setFormOpen(true) }}><Pencil size={16} /></Button>
              {student.status === 'ACTIVE' && <ConfirmDialog title="Deactivate student profile?" description={`The academic profile for ${student.firstName} ${student.lastName ?? ''} will be marked inactive. The linked user account is unchanged.`} confirmLabel="Deactivate profile" onConfirm={async () => { try { await deactivate.mutateAsync(student.studentId) } catch (error) { notifyError(error, 'The student could not be deactivated.'); throw error } }} trigger={<Button size="sm" variant="ghost" aria-label={`Deactivate ${student.firstName}`} title="Deactivate profile"><UserX size={16} /></Button>} />}
            </div>
          )} />
        )}
        {!students.isPending && !students.isError && students.data && <PaginationControls page={students.data.page} size={students.data.size} totalElements={students.data.totalElements} totalPages={students.data.totalPages} onPageChange={setPage} />}
      </Card>
      <StudentFormDialog open={formOpen} onOpenChange={setFormOpen} student={editing} users={availableUsers} departments={departments.data ?? []} onSubmit={save} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`${viewing.firstName} ${viewing.lastName ?? ''}`} description="Student academic profile" fields={[
        { label: 'Student ID', value: viewing.studentId }, { label: 'Enrollment number', value: viewing.enrollmentNumber },
        { label: 'Account', value: userById.get(viewing.userId)?.username ?? `User #${viewing.userId}` },
        { label: 'Department', value: departmentById.get(viewing.departmentId)?.name ?? `Department #${viewing.departmentId}` },
        { label: 'Admission year', value: viewing.admissionYear }, { label: 'Date of birth', value: viewing.dateOfBirth },
        { label: 'Phone', value: viewing.phone }, { label: 'Status', value: viewing.status },
      ]} />}
    </div>
  )
}
