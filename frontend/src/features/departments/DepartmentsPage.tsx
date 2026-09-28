import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { notifyError } from '../admin-shared/feedback'
import { createDepartment, deleteDepartment, departmentKeys, getDepartments, updateDepartment } from './departments.api'
import { DepartmentFormDialog } from './DepartmentFormDialog'
import type { Department, DepartmentRequest } from '../../types/api'

export function DepartmentsPage() {
  const client = useQueryClient()
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const [search, setSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Department | null>(null)
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  const create = useMutation({ mutationFn: createDepartment, onSuccess: async () => { await refresh(); setFormOpen(false); toast.success('Department created') } })
  const update = useMutation({ mutationFn: ({ id, request }: { id: number; request: DepartmentRequest }) => updateDepartment(id, request), onSuccess: async () => { await refresh(); setFormOpen(false); setEditing(null); toast.success('Department updated') } })
  const remove = useMutation({ mutationFn: deleteDepartment, onSuccess: async () => { await refresh(); toast.success('Department deleted') } })
  const filtered = useMemo(() => (departments.data ?? []).filter((department) => `${department.name} ${department.code}`.toLowerCase().includes(search.trim().toLowerCase())), [departments.data, search])

  async function save(request: DepartmentRequest) {
    try { if (editing) await update.mutateAsync({ id: editing.departmentId, request }); else await create.mutateAsync(request) }
    catch (error) { notifyError(error, 'The department could not be saved.') }
  }

  const columns = [
    { key: 'department', header: 'Department', render: (department: Department) => <div className="identity-cell"><span className="entity-icon"><Building2 size={18} aria-hidden="true" /></span><span><strong>{department.name}</strong><small>Academic unit</small></span></div> },
    { key: 'code', header: 'Code', render: (department: Department) => <span className="mono-label">{department.code}</span> },
    { key: 'id', header: 'Record ID', mobileHidden: true, render: (department: Department) => <span className="muted">#{department.departmentId}</span> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic structure" title="Departments" description="Organize academic units and maintain their department codes." action={<Button onClick={() => { setEditing(null); setFormOpen(true) }}><Plus size={17} aria-hidden="true" /> Add department</Button>} />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search departments" countLabel={`${filtered.length} of ${departments.data?.length ?? 0} departments`} />
        {departments.isPending ? <LoadingState label="Loading departments" /> : departments.isError ? <ErrorState error={departments.error} onRetry={() => void departments.refetch()} /> : filtered.length === 0 ? <EmptyState title={departments.data.length === 0 ? 'No departments yet' : 'No departments match this search'} description={departments.data.length === 0 ? 'Add the first academic department to begin organizing courses and people.' : 'Try a different department name or code.'} /> : (
          <ResourceTable caption="Department directory" columns={columns} rows={filtered} getRowKey={(department) => department.departmentId} actions={(department) => (
            <div className="row-actions">
              <Button size="sm" variant="ghost" aria-label={`Edit ${department.name}`} title="Edit department" onClick={() => { setEditing(department); setFormOpen(true) }}><Pencil size={16} /></Button>
              <ConfirmDialog title="Delete department?" description={`Delete ${department.name} (${department.code})? The backend may reject deletion while related records still reference this department.`} confirmLabel="Delete department" onConfirm={async () => { try { await remove.mutateAsync(department.departmentId) } catch (error) { notifyError(error, 'The department could not be deleted.'); throw error } }} trigger={<Button size="sm" variant="ghost" aria-label={`Delete ${department.name}`} title="Delete department"><Trash2 size={16} /></Button>} />
            </div>
          )} />
        )}
      </Card>
      <DepartmentFormDialog open={formOpen} onOpenChange={setFormOpen} department={editing} onSubmit={save} />
    </div>
  )
}
