import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Pencil, Plus, Trash2 } from 'lucide-react'
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
import { TableSkeleton } from '../../components/ui/Skeleton'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { notifyError } from '../admin-shared/feedback'
import { formatDate } from '../../lib/format/datetime'
import { courseKeys, getCourses } from '../courses/courses.api'
import { getStudents, studentKeys } from '../students/students.api'
import { EnrollmentFormDialog } from './EnrollmentFormDialog'
import { EnrollmentEditDialog } from './EnrollmentEditDialog'
import {
  createEnrollment,
  deleteEnrollment,
  enrollmentKeys,
  getEnrollment,
  getEnrollmentsPage,
  updateEnrollment,
} from './enrollments.api'
import type { EnrollmentUpdateRequest } from './enrollments.api'
import type { EnrollmentResponse } from '../../types/api'

export function EnrollmentsPage() {
  const client = useQueryClient()
  const [page, setPage] = useState(0)
  const pageSize = 20
  const enrollments = useQuery({ queryKey: enrollmentKeys.page(page, pageSize), queryFn: () => getEnrollmentsPage(page, pageSize) })
  const students = useQuery({ queryKey: studentKeys.all, queryFn: getStudents })
  const courses = useQuery({ queryKey: courseKeys.all, queryFn: getCourses })
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<EnrollmentResponse | null>(null)
  const [viewing, setViewing] = useState<EnrollmentResponse | null>(null)
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  const create = useMutation({ mutationFn: createEnrollment, onSuccess: async () => { await refresh(); setFormOpen(false); toast.success('Enrollment created') } })
  const update = useMutation({
    mutationFn: ({ id, request }: { id: number; request: EnrollmentUpdateRequest }) => updateEnrollment(id, request),
    onSuccess: async () => { await refresh(); setEditing(null); toast.success('Enrollment updated') },
  })
  const remove = useMutation({ mutationFn: deleteEnrollment, onSuccess: async () => { await refresh(); toast.success('Enrollment deleted') } })
  const detail = useQuery({ queryKey: enrollmentKeys.detail(viewing?.enrollmentId ?? 0), queryFn: () => getEnrollment(viewing!.enrollmentId), enabled: viewing !== null })
  const studentById = useMemo(() => new Map((students.data ?? []).map((student) => [student.studentId, student])), [students.data])
  const courseById = useMemo(() => new Map((courses.data ?? []).map((course) => [course.courseId, course])), [courses.data])
  const filtered = useMemo(() => (enrollments.data?.content ?? []).filter((enrollment) => {
    const student = studentById.get(enrollment.studentId)
    const course = courseById.get(enrollment.courseId)
    const match = `${student?.enrollmentNumber ?? ''} ${student?.firstName ?? ''} ${student?.lastName ?? ''} ${course?.courseCode ?? ''} ${course?.courseName ?? ''} ${enrollment.semester} ${enrollment.academicYear}`.toLowerCase().includes(search.trim().toLowerCase())
    return match && (status === 'ALL' || enrollment.status === status)
  }), [enrollments.data, studentById, courseById, search, status])

  async function save(request: Parameters<typeof createEnrollment>[0]) {
    try { await create.mutateAsync(request) } catch (error) { notifyError(error, 'The enrollment could not be created.') }
  }

  async function saveEdit(enrollmentId: number, request: EnrollmentUpdateRequest) {
    try { await update.mutateAsync({ id: enrollmentId, request }) } catch (error) { notifyError(error, 'The enrollment could not be updated.') }
  }

  const columns = [
    { key: 'student', header: 'Student', render: (enrollment: EnrollmentResponse) => { const student = studentById.get(enrollment.studentId); return <div className="identity-cell"><span className="identity-avatar identity-avatar--teal" aria-hidden="true">{student?.firstName?.slice(0, 1) ?? '—'}</span><span><strong>{student ? `${student.firstName} ${student.lastName ?? ''}` : `Student #${enrollment.studentId}`}</strong><small>{student?.enrollmentNumber ?? 'Enrollment profile unavailable'}</small></span></div> } },
    { key: 'course', header: 'Course', render: (enrollment: EnrollmentResponse) => { const course = courseById.get(enrollment.courseId); return <span><strong>{course?.courseCode ?? `Course #${enrollment.courseId}`}</strong><small className="cell-subtext">{course?.courseName ?? 'Course details unavailable'}</small></span> } },
    { key: 'term', header: 'Term', render: (enrollment: EnrollmentResponse) => `${enrollment.semester} · ${enrollment.academicYear}` },
    { key: 'date', header: 'Enrolled', mobileHidden: true, render: (enrollment: EnrollmentResponse) => formatDate(enrollment.enrollmentDate) ?? <span className="muted">—</span> },
    { key: 'status', header: 'Status', render: (enrollment: EnrollmentResponse) => <StatusBadge status={enrollment.status} /> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic operations" title="Enrollments" description="Review course registrations and create new student-course enrollments." action={<Button onClick={() => setFormOpen(true)} disabled={students.isPending || students.isError || courses.isPending || courses.isError}><Plus size={17} aria-hidden="true" /> Create enrollment</Button>} />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={(value) => { setSearch(value); setPage(0) }} searchLabel="Search this page" countLabel={`${filtered.length} shown · ${enrollments.data?.totalElements ?? 0} total`} filters={<SelectField label="Status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="ALL">All statuses</option><option value="ENROLLED">Enrolled</option><option value="DROPPED">Dropped</option><option value="COMPLETED">Completed</option></SelectField>} />
        {enrollments.isPending ? <TableSkeleton rows={8} columns={5} label="Loading enrollments" /> : enrollments.isError ? <ErrorState error={enrollments.error} onRetry={() => void enrollments.refetch()} /> : filtered.length === 0 ? <EmptyState title={enrollments.data.totalElements ? 'No enrollments on this page match' : 'No enrollments yet'} description={enrollments.data.totalElements ? 'Try another search, status or page.' : 'Create an enrollment from an active student and active course.'} /> : (
          <ResourceTable caption="Enrollment directory" columns={columns} rows={filtered} getRowKey={(enrollment) => enrollment.enrollmentId} actions={(enrollment) => (
            <div className="row-actions">
              <Button size="sm" variant="ghost" title="View enrollment details" aria-label={`View enrollment ${enrollment.enrollmentId}`} onClick={() => setViewing(enrollment)}><Eye size={16} aria-hidden="true" /></Button>
              <Button size="sm" variant="ghost" title="Edit enrollment" aria-label={`Edit enrollment ${enrollment.enrollmentId}`} onClick={() => setEditing(enrollment)}><Pencil size={16} aria-hidden="true" /></Button>
              <ConfirmDialog
                title="Delete enrollment?"
                description={`Delete this enrollment of ${studentById.get(enrollment.studentId)?.enrollmentNumber ?? `student #${enrollment.studentId}`}? Deletion is refused once the enrollment has marks or attendance records; change the status instead to record the outcome.`}
                confirmLabel="Delete enrollment"
                onConfirm={async () => {
                  try { await remove.mutateAsync(enrollment.enrollmentId) }
                  catch (error) { notifyError(error, 'The enrollment could not be deleted.'); throw error }
                }}
                trigger={<Button size="sm" variant="ghost" title="Delete enrollment" aria-label={`Delete enrollment ${enrollment.enrollmentId}`}><Trash2 size={16} aria-hidden="true" /></Button>}
              />
            </div>
          )} />
        )}
        {!enrollments.isPending && !enrollments.isError && enrollments.data && <PaginationControls page={enrollments.data.page} size={enrollments.data.size} totalElements={enrollments.data.totalElements} totalPages={enrollments.data.totalPages} onPageChange={setPage} />}
      </Card>
      {(students.isError || courses.isError) && <ErrorState error={students.error ?? courses.error} onRetry={() => { void students.refetch(); void courses.refetch() }} />}
      <EnrollmentEditDialog open={editing !== null} onOpenChange={(open) => { if (!open) setEditing(null) }} enrollment={editing} onSubmit={saveEdit} />
      <EnrollmentFormDialog open={formOpen} onOpenChange={setFormOpen} students={(students.data ?? []).filter((student) => student.status === 'ACTIVE')} courses={(courses.data ?? []).filter((course) => course.status === 'ACTIVE')} onSubmit={save} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`Enrollment #${viewing.enrollmentId}`} description="Course registration details" notice={detail.isPending ? <LoadingState label="Refreshing enrollment details" /> : detail.isError ? <ErrorState error={detail.error} onRetry={() => void detail.refetch()} /> : undefined} fields={[
        { label: 'Enrollment ID', value: detail.data?.enrollmentId ?? viewing.enrollmentId },
        { label: 'Student', value: studentById.get(detail.data?.studentId ?? viewing.studentId)?.enrollmentNumber ?? `Student #${detail.data?.studentId ?? viewing.studentId}` },
        { label: 'Course', value: courseById.get(detail.data?.courseId ?? viewing.courseId)?.courseCode ?? `Course #${detail.data?.courseId ?? viewing.courseId}` },
        { label: 'Semester', value: detail.data?.semester ?? viewing.semester }, { label: 'Academic year', value: detail.data?.academicYear ?? viewing.academicYear },
        { label: 'Enrollment date', value: formatDate(detail.data?.enrollmentDate ?? viewing.enrollmentDate) ?? '—' }, { label: 'Status', value: detail.data?.status ?? viewing.status },
      ]} />}
    </div>
  )
}
