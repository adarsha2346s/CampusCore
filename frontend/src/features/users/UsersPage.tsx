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
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { notifyError } from '../admin-shared/feedback'
import { UserFormDialog } from './UserFormDialog'
import { createUser, deactivateUser, getUsersPage, updateUser, userKeys } from './users.api'
import type { Role, UserRequest, UserResponse } from '../../types/api'

const roleLabels: Record<Role, string> = { ADMIN: 'Administrator', FACULTY: 'Faculty', STUDENT: 'Student' }

export function UsersPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const pageSize = 20
  const users = useQuery({ queryKey: userKeys.page(page, pageSize), queryFn: () => getUsersPage(page, pageSize) })
  const [search, setSearch] = useState('')
  const [role, setRole] = useState('ALL')
  const [active, setActive] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<UserResponse | null>(null)
  const [viewing, setViewing] = useState<UserResponse | null>(null)
  const [notice, setNotice] = useState('')

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin'] })
  const create = useMutation({ mutationFn: createUser, onSuccess: async () => { await refresh(); setFormOpen(false); setEditing(null); setNotice('User account created.'); toast.success('User account created') } })
  const update = useMutation({ mutationFn: ({ id, request }: { id: number; request: UserRequest }) => updateUser(id, request), onSuccess: async () => { await refresh(); setFormOpen(false); setEditing(null); setNotice('User account updated.'); toast.success('User account updated') } })
  const deactivate = useMutation({ mutationFn: deactivateUser, onSuccess: async () => { await refresh(); setNotice('User account deactivated.'); toast.success('User account deactivated') } })

  const filtered = useMemo(() => (users.data?.content ?? []).filter((user) => {
    const matchSearch = `${user.username} ${user.email}`.toLowerCase().includes(search.trim().toLowerCase())
    return matchSearch && (role === 'ALL' || user.role === role) && (active === 'ALL' || String(user.active) === active)
  }), [users.data, search, role, active])

  async function save(request: UserRequest) {
    try {
      if (editing) await update.mutateAsync({ id: editing.userId, request })
      else await create.mutateAsync(request)
    } catch (error) { notifyError(error, 'The user account could not be saved.') }
  }

  const columns = [
    { key: 'username', header: 'Account', render: (user: UserResponse) => <div className="identity-cell"><span className="identity-avatar" aria-hidden="true">{user.username.slice(0, 1).toUpperCase()}</span><span><strong>{user.username}</strong><small>{user.email}</small></span></div> },
    { key: 'role', header: 'Role', render: (user: UserResponse) => <Badge className="role-badge">{roleLabels[user.role]}</Badge> },
    { key: 'active', header: 'Status', render: (user: UserResponse) => <StatusBadge status={user.active ? 'ACTIVE' : 'INACTIVE'} /> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Access management" title="Users" description="Manage account identity, role access and active status." action={<Button onClick={() => { setEditing(null); setFormOpen(true) }}><Plus size={17} aria-hidden="true" /> Add user</Button>} />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={(value) => { setSearch(value); setPage(0) }} searchLabel="Search this page" countLabel={`${filtered.length} shown · ${users.data?.totalElements ?? 0} total`} filters={(
          <><SelectField label="Role" value={role} onChange={(event) => { setRole(event.target.value); setPage(0) }}><option value="ALL">All roles</option><option value="ADMIN">Administrators</option><option value="FACULTY">Faculty</option><option value="STUDENT">Students</option></SelectField><SelectField label="Status" value={active} onChange={(event) => { setActive(event.target.value); setPage(0) }}><option value="ALL">All statuses</option><option value="true">Active</option><option value="false">Inactive</option></SelectField></>
        )} />
        {notice && <p className="sr-only" role="status">{notice}</p>}
        {users.isPending ? <LoadingState label="Loading user accounts" /> : users.isError ? <ErrorState error={users.error} onRetry={() => void users.refetch()} /> : filtered.length === 0 ? <EmptyState title={users.data.totalElements === 0 ? 'No user accounts yet' : 'No accounts on this page match these filters'} description={users.data.totalElements === 0 ? 'Create an account to get started.' : 'Try changing filters or moving to another page.'} /> : (
          <ResourceTable caption="Campus user accounts" columns={columns} rows={filtered} getRowKey={(user) => user.userId} actions={(user) => (
            <div className="row-actions">
              <Button size="sm" variant="ghost" aria-label={`View ${user.username}`} title="View details" onClick={() => setViewing(user)}><Eye size={16} /></Button>
              <Button size="sm" variant="ghost" aria-label={`Edit ${user.username}`} title="Edit account" onClick={() => { setEditing(user); setFormOpen(true) }}><Pencil size={16} /></Button>
              {user.active && <ConfirmDialog title="Deactivate account?" description={`The account for ${user.username} will no longer be active. The API does not currently provide a reactivation operation.`} confirmLabel="Deactivate account" onConfirm={async () => { try { await deactivate.mutateAsync(user.userId) } catch (error) { notifyError(error, 'The account could not be deactivated.'); throw error } }} trigger={<Button size="sm" variant="ghost" aria-label={`Deactivate ${user.username}`} title="Deactivate account"><UserX size={16} /></Button>} />}
            </div>
          )} />
        )}
        {!users.isPending && !users.isError && users.data && <PaginationControls page={users.data.page} size={users.data.size} totalElements={users.data.totalElements} totalPages={users.data.totalPages} onPageChange={setPage} />}
      </Card>
      <UserFormDialog open={formOpen} onOpenChange={setFormOpen} user={editing} onSubmit={save} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={viewing.username} description="User account details" fields={[{ label: 'User ID', value: viewing.userId }, { label: 'Email', value: viewing.email }, { label: 'Role', value: roleLabels[viewing.role] }, { label: 'Status', value: viewing.active ? 'Active' : 'Inactive' }]} />}
    </div>
  )
}
