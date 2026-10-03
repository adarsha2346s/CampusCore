import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BookOpen, Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { PaginationControls } from '../../components/data-display/PaginationControls'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { notifyError } from '../admin-shared/feedback'
import { departmentKeys, getDepartments } from '../departments/departments.api'
import { CourseFormDialog } from './CourseFormDialog'
import { createCourse, courseKeys, deleteCourse, getCoursesPage, updateCourse } from './courses.api'
import type { CourseRequest, CourseResponse } from '../../types/api'

export function CoursesPage() {
  const client = useQueryClient()
  const [page, setPage] = useState(0)
  const pageSize = 20
  const courses = useQuery({ queryKey: courseKeys.page(page, pageSize), queryFn: () => getCoursesPage(page, pageSize) })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [departmentId, setDepartmentId] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<CourseResponse | null>(null)
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  const create = useMutation({ mutationFn: createCourse, onSuccess: async () => { await refresh(); setFormOpen(false); toast.success('Course created') } })
  const update = useMutation({ mutationFn: ({ id, request }: { id: number; request: CourseRequest }) => updateCourse(id, request), onSuccess: async () => { await refresh(); setFormOpen(false); setEditing(null); toast.success('Course updated') } })
  const remove = useMutation({ mutationFn: deleteCourse, onSuccess: async () => { await refresh(); toast.success('Course deleted') } })
  const departmentById = useMemo(() => new Map((departments.data ?? []).map((department) => [department.departmentId, department])), [departments.data])
  const filtered = useMemo(() => (courses.data?.content ?? []).filter((course) => {
    const department = departmentById.get(course.departmentId)
    const matchSearch = `${course.courseCode} ${course.courseName} ${department?.name ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())
    return matchSearch && (status === 'ALL' || course.status === status) && (departmentId === 'ALL' || String(course.departmentId) === departmentId)
  }), [courses.data, departmentById, search, status, departmentId])

  async function save(request: CourseRequest) {
    try { if (editing) await update.mutateAsync({ id: editing.courseId, request }); else await create.mutateAsync(request) }
    catch (error) { notifyError(error, 'The course could not be saved.') }
  }

  const columns = [
    { key: 'course', header: 'Course', render: (course: CourseResponse) => <div className="identity-cell"><span className="entity-icon entity-icon--blue"><BookOpen size={18} aria-hidden="true" /></span><span><strong>{course.courseName}</strong><small className="mono-label">{course.courseCode}</small></span></div> },
    { key: 'department', header: 'Department', render: (course: CourseResponse) => departmentById.get(course.departmentId)?.name ?? `Department #${course.departmentId}` },
    { key: 'credits', header: 'Credits', render: (course: CourseResponse) => course.credits },
    { key: 'capacity', header: 'Capacity', render: (course: CourseResponse) => course.capacity.toLocaleString() },
    { key: 'status', header: 'Status', render: (course: CourseResponse) => <StatusBadge status={course.status} /> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic catalog" title="Courses" description="Maintain catalog details, credit values and enrollment capacity." action={<Button onClick={() => { setEditing(null); setFormOpen(true) }} disabled={departments.isPending || departments.isError || departments.data?.length === 0}><Plus size={17} aria-hidden="true" /> Add course</Button>} />
      {departments.isError && <ErrorState error={departments.error} onRetry={() => void departments.refetch()} />}
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={(value) => { setSearch(value); setPage(0) }} searchLabel="Search this page" countLabel={`${filtered.length} shown · ${courses.data?.totalElements ?? 0} total`} filters={(
          <><SelectField label="Status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></SelectField><SelectField label="Department" value={departmentId} onChange={(event) => { setDepartmentId(event.target.value); setPage(0) }}><option value="ALL">All departments</option>{departments.data?.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.code}</option>)}</SelectField></>
        )} />
        {courses.isPending ? <TableSkeleton rows={8} columns={5} label="Loading courses" /> : courses.isError ? <ErrorState error={courses.error} onRetry={() => void courses.refetch()} /> : filtered.length === 0 ? <EmptyState title={courses.data.totalElements === 0 ? 'No courses yet' : 'No courses on this page match these filters'} description={courses.data.totalElements === 0 ? 'Create a department first, then add its courses to the catalog.' : 'Try another search, status, department or page.'} /> : (
          <ResourceTable caption="Course catalog" columns={columns} rows={filtered} getRowKey={(course) => course.courseId} actions={(course) => (
            <div className="row-actions">
              <Button size="sm" variant="ghost" aria-label={`Edit ${course.courseCode}`} title="Edit course" onClick={() => { setEditing(course); setFormOpen(true) }}><Pencil size={16} /></Button>
              <ConfirmDialog title="Delete course?" description={`Delete ${course.courseCode} — ${course.courseName}? The backend may reject deletion while related enrollments or assessments exist.`} confirmLabel="Delete course" onConfirm={async () => { try { await remove.mutateAsync(course.courseId) } catch (error) { notifyError(error, 'The course could not be deleted.'); throw error } }} trigger={<Button size="sm" variant="ghost" aria-label={`Delete ${course.courseCode}`} title="Delete course"><Trash2 size={16} /></Button>} />
            </div>
          )} />
        )}
        {!courses.isPending && !courses.isError && courses.data && <PaginationControls page={courses.data.page} size={courses.data.size} totalElements={courses.data.totalElements} totalPages={courses.data.totalPages} onPageChange={setPage} />}
      </Card>
      <CourseFormDialog open={formOpen} onOpenChange={setFormOpen} course={editing} departments={departments.data ?? []} onSubmit={save} />
    </div>
  )
}
