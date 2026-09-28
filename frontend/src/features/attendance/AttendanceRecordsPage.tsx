import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { notifyError } from '../admin-shared/feedback'
import { courseKeys, getCourses } from '../courses/courses.api'
import { enrollmentKeys, getEnrollments } from '../enrollments/enrollments.api'
import { studentKeys, getStudents } from '../students/students.api'
import { AttendanceRecordFormDialog } from './AttendanceRecordFormDialog'
import { attendanceRecordKeys, attendanceSessionKeys, createAttendanceRecord, getAttendanceRecord, getAttendanceRecords, getAttendanceRecordsByEnrollment, getAttendanceRecordsBySession, getAttendanceSessions } from './attendance.api'
import type { AttendanceRecordResponse } from '../../types/api'

export function AttendanceRecordsPage() {
  const client = useQueryClient()
  const [search, setSearch] = useState('')
  const [sessionId, setSessionId] = useState('ALL')
  const [enrollmentId, setEnrollmentId] = useState('ALL')
  const [status, setStatus] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [viewing, setViewing] = useState<AttendanceRecordResponse | null>(null)
  const records = useQuery({
    queryKey: sessionId !== 'ALL' ? attendanceRecordKeys.bySession(sessionId) : enrollmentId !== 'ALL' ? attendanceRecordKeys.byEnrollment(enrollmentId) : attendanceRecordKeys.all,
    queryFn: () => sessionId !== 'ALL' ? getAttendanceRecordsBySession(Number(sessionId)) : enrollmentId !== 'ALL' ? getAttendanceRecordsByEnrollment(Number(enrollmentId)) : getAttendanceRecords(),
  })
  const sessions = useQuery({ queryKey: attendanceSessionKeys.all, queryFn: getAttendanceSessions })
  const enrollments = useQuery({ queryKey: enrollmentKeys.all, queryFn: getEnrollments })
  const students = useQuery({ queryKey: studentKeys.all, queryFn: getStudents })
  const courses = useQuery({ queryKey: courseKeys.all, queryFn: getCourses })
  const create = useMutation({ mutationFn: createAttendanceRecord, onSuccess: async () => { await client.invalidateQueries({ queryKey: ['admin'] }); setFormOpen(false); toast.success('Attendance record created') } })
  const detail = useQuery({ queryKey: attendanceRecordKeys.detail(viewing?.attendanceRecordId ?? 0), queryFn: () => getAttendanceRecord(viewing!.attendanceRecordId), enabled: viewing !== null })
  const sessionById = useMemo(() => new Map((sessions.data ?? []).map((item) => [item.attendanceSessionId, item])), [sessions.data])
  const enrollmentById = useMemo(() => new Map((enrollments.data ?? []).map((item) => [item.enrollmentId, item])), [enrollments.data])
  const studentById = useMemo(() => new Map((students.data ?? []).map((item) => [item.studentId, item])), [students.data])
  const courseById = useMemo(() => new Map((courses.data ?? []).map((item) => [item.courseId, item])), [courses.data])
  const filtered = useMemo(() => (records.data ?? []).filter((record) => {
    const session = sessionById.get(record.attendanceSessionId)
    const enrollment = enrollmentById.get(record.enrollmentId)
    const student = enrollment ? studentById.get(enrollment.studentId) : undefined
    const course = session ? courseById.get(session.courseId) : undefined
    const match = `${record.attendanceRecordId} ${record.status} ${session?.sessionDate ?? ''} ${session?.topic ?? ''} ${student?.enrollmentNumber ?? ''} ${course?.courseCode ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())
    return match && (enrollmentId === 'ALL' || record.enrollmentId === Number(enrollmentId)) && (status === 'ALL' || record.status === status)
  }), [records.data, sessionById, enrollmentById, studentById, courseById, search, enrollmentId, status])

  async function save(request: Parameters<typeof createAttendanceRecord>[0]) {
    try { await create.mutateAsync(request) } catch (error) { notifyError(error, 'The attendance record could not be created.') }
  }
  const columns = [
    { key: 'student', header: 'Student', render: (record: AttendanceRecordResponse) => { const enrollment = enrollmentById.get(record.enrollmentId); const student = enrollment ? studentById.get(enrollment.studentId) : undefined; return <div className="identity-cell"><span className="identity-avatar identity-avatar--teal" aria-hidden="true">{student?.firstName?.slice(0, 1) ?? '—'}</span><span><strong>{student?.enrollmentNumber ?? `Enrollment #${record.enrollmentId}`}</strong><small>{student ? `${student.firstName} ${student.lastName ?? ''}` : `Enrollment #${record.enrollmentId}`}</small></span></div> } },
    { key: 'session', header: 'Session', render: (record: AttendanceRecordResponse) => { const session = sessionById.get(record.attendanceSessionId); const course = session ? courseById.get(session.courseId) : undefined; return session ? `${session.sessionDate} · ${course?.courseCode ?? `Course #${session.courseId}`}` : `Session #${record.attendanceSessionId}` } },
    { key: 'status', header: 'Status', render: (record: AttendanceRecordResponse) => <StatusBadge status={record.status} /> },
  ]
  const dependencyError = sessions.error ?? enrollments.error ?? students.error ?? courses.error
  const dependenciesPending = sessions.isPending || enrollments.isPending || students.isPending || courses.isPending
  const recordSession = viewing ? sessionById.get(viewing.attendanceSessionId) : undefined
  const recordEnrollment = viewing ? enrollmentById.get(viewing.enrollmentId) : undefined
  const recordStudent = recordEnrollment ? studentById.get(recordEnrollment.studentId) : undefined

  return (
    <div className="admin-page">
      <nav className="attendance-subnav" aria-label="Attendance sections"><Link to="/admin/attendance/sessions">Sessions</Link><span className="attendance-subnav__active" aria-current="page">Records</span></nav>
      <PageHeader eyebrow="Academic operations" title="Attendance records" description="Review individual attendance entries. The backend accepts one enrollment record per request." action={<Button onClick={() => setFormOpen(true)} disabled={dependenciesPending || Boolean(dependencyError) || !sessions.data?.length || !enrollments.data?.length}><Plus size={17} aria-hidden="true" /> Record attendance</Button>} />
      {dependencyError && <ErrorState error={dependencyError} onRetry={() => { void sessions.refetch(); void enrollments.refetch(); void students.refetch(); void courses.refetch() }} />}
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search attendance records" countLabel={`${filtered.length} matching records`} filters={(
          <><SelectField label="Session" value={sessionId} onChange={(event) => setSessionId(event.target.value)}><option value="ALL">All sessions</option>{sessions.data?.map((session) => <option key={session.attendanceSessionId} value={session.attendanceSessionId}>{session.sessionDate} · #{session.attendanceSessionId}</option>)}</SelectField><SelectField label="Enrollment" value={enrollmentId} onChange={(event) => setEnrollmentId(event.target.value)}><option value="ALL">All enrollments</option>{enrollments.data?.map((enrollment) => <option key={enrollment.enrollmentId} value={enrollment.enrollmentId}>#{enrollment.enrollmentId} · {studentById.get(enrollment.studentId)?.enrollmentNumber ?? `Student #${enrollment.studentId}`}</option>)}</SelectField><SelectField label="Status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All statuses</option><option value="PRESENT">Present</option><option value="ABSENT">Absent</option><option value="LATE">Late</option></SelectField></>
        )} />
        {records.isPending ? <LoadingState label="Loading attendance records" /> : records.isError ? <ErrorState error={records.error} onRetry={() => void records.refetch()} /> : !filtered.length ? <EmptyState title={records.data.length ? 'No records match these filters' : 'No attendance records yet'} description={records.data.length ? 'Try another session, enrollment or attendance status.' : 'Create a session and add attendance records one enrollment at a time.'} /> : (
          <ResourceTable caption="Attendance records" columns={columns} rows={filtered} getRowKey={(record) => record.attendanceRecordId} actions={(record) => <Button size="sm" variant="ghost" title="View attendance record" aria-label={`View attendance record ${record.attendanceRecordId}`} onClick={() => setViewing(record)}><Eye size={16} /></Button>} />
        )}
      </Card>
      <AttendanceRecordFormDialog open={formOpen} onOpenChange={setFormOpen} sessions={sessions.data ?? []} enrollments={enrollments.data ?? []} students={students.data ?? []} courses={courses.data ?? []} onSubmit={save} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`Attendance record #${viewing.attendanceRecordId}`} description="Attendance entry details" notice={detail.isPending ? <LoadingState label="Refreshing attendance details" /> : detail.isError ? <ErrorState error={detail.error} onRetry={() => void detail.refetch()} /> : undefined} fields={[
        { label: 'Record ID', value: detail.data?.attendanceRecordId ?? viewing.attendanceRecordId }, { label: 'Session', value: recordSession ? `${recordSession.sessionDate}${recordSession.topic ? ` · ${recordSession.topic}` : ''}` : `Session #${viewing.attendanceSessionId}` },
        { label: 'Course', value: recordSession ? courseById.get(recordSession.courseId)?.courseCode ?? `Course #${recordSession.courseId}` : '—' },
        { label: 'Student', value: recordStudent ? `${recordStudent.firstName} ${recordStudent.lastName ?? ''} · ${recordStudent.enrollmentNumber}` : `Enrollment #${viewing.enrollmentId}` },
        { label: 'Status', value: detail.data?.status ?? viewing.status },
      ]} />}
    </div>
  )
}
