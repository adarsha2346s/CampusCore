import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarDays, Eye, Plus } from 'lucide-react'
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
import { notifyError } from '../admin-shared/feedback'
import { courseKeys, getCourses } from '../courses/courses.api'
import { facultyKeys, getFaculty } from '../faculty/faculty.api'
import { AttendanceSessionDetailsDialog } from './AttendanceSessionDetailsDialog'
import { AttendanceSessionFormDialog } from './AttendanceSessionFormDialog'
import { attendanceSessionKeys, createAttendanceSession, getAttendanceSessions, getAttendanceSessionsByCourse, getAttendanceSessionsByFaculty } from './attendance.api'
import type { AttendanceSessionResponse } from '../../types/api'

export function AttendanceSessionsPage() {
  const client = useQueryClient()
  const courses = useQuery({ queryKey: courseKeys.all, queryFn: getCourses })
  const faculty = useQuery({ queryKey: facultyKeys.all, queryFn: getFaculty })
  const [search, setSearch] = useState('')
  const [courseId, setCourseId] = useState('ALL')
  const [facultyId, setFacultyId] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [viewing, setViewing] = useState<AttendanceSessionResponse | null>(null)
  const sessions = useQuery({
    queryKey: courseId !== 'ALL' ? attendanceSessionKeys.byCourse(courseId) : facultyId !== 'ALL' ? attendanceSessionKeys.byFaculty(facultyId) : attendanceSessionKeys.all,
    queryFn: () => courseId !== 'ALL' ? getAttendanceSessionsByCourse(Number(courseId)) : facultyId !== 'ALL' ? getAttendanceSessionsByFaculty(Number(facultyId)) : getAttendanceSessions(),
  })
  const create = useMutation({ mutationFn: createAttendanceSession, onSuccess: async () => { await client.invalidateQueries({ queryKey: ['admin'] }); setFormOpen(false); toast.success('Attendance session created') } })
  const courseById = useMemo(() => new Map((courses.data ?? []).map((course) => [course.courseId, course])), [courses.data])
  const facultyById = useMemo(() => new Map((faculty.data ?? []).map((member) => [member.facultyId, member])), [faculty.data])
  const filtered = useMemo(() => (sessions.data ?? []).filter((session) => {
    const course = courseById.get(session.courseId)
    const member = facultyById.get(session.facultyId)
    const match = `${session.topic ?? ''} ${session.sessionDate} ${course?.courseCode ?? ''} ${course?.courseName ?? ''} ${member?.firstName ?? ''} ${member?.lastName ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())
    return match && (facultyId === 'ALL' || session.facultyId === Number(facultyId))
  }), [sessions.data, courseById, facultyById, search, facultyId])

  async function save(request: Parameters<typeof createAttendanceSession>[0]) {
    try { await create.mutateAsync(request) } catch (error) { notifyError(error, 'The attendance session could not be created.') }
  }

  const columns = [
    { key: 'date', header: 'Session', render: (session: AttendanceSessionResponse) => <div className="identity-cell"><span className="entity-icon entity-icon--blue"><CalendarDays size={17} aria-hidden="true" /></span><span><strong>{session.sessionDate}</strong><small>{session.topic || 'No topic provided'}</small></span></div> },
    { key: 'course', header: 'Course', render: (session: AttendanceSessionResponse) => { const course = courseById.get(session.courseId); return course ? `${course.courseCode} · ${course.courseName}` : `Course #${session.courseId}` } },
    { key: 'faculty', header: 'Faculty', render: (session: AttendanceSessionResponse) => { const member = facultyById.get(session.facultyId); return member ? `${member.firstName} ${member.lastName ?? ''}` : `Faculty #${session.facultyId}` } },
  ]
  const dependencyError = courses.error ?? faculty.error
  const dependenciesPending = courses.isPending || faculty.isPending

  return (
    <div className="admin-page">
      <nav className="attendance-subnav" aria-label="Attendance sections"><span className="attendance-subnav__active" aria-current="page">Sessions</span><Link to="/admin/attendance/records">Records</Link></nav>
      <PageHeader eyebrow="Academic operations" title="Attendance sessions" description="Schedule course attendance sessions and review the records captured for each session." action={<Button onClick={() => setFormOpen(true)} disabled={dependenciesPending || Boolean(dependencyError) || !courses.data?.some((course) => course.status === 'ACTIVE') || !faculty.data?.some((member) => member.status === 'ACTIVE')}><Plus size={17} aria-hidden="true" /> Create session</Button>} />
      {dependencyError && <ErrorState error={dependencyError} onRetry={() => { void courses.refetch(); void faculty.refetch() }} />}
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search sessions" countLabel={`${filtered.length} matching sessions`} filters={(
          <><SelectField label="Course" value={courseId} onChange={(event) => setCourseId(event.target.value)}><option value="ALL">All courses</option>{courses.data?.map((course) => <option key={course.courseId} value={course.courseId}>{course.courseCode}</option>)}</SelectField><SelectField label="Faculty" value={facultyId} onChange={(event) => setFacultyId(event.target.value)}><option value="ALL">All faculty</option>{faculty.data?.map((member) => <option key={member.facultyId} value={member.facultyId}>{member.employeeNumber} · {member.firstName}</option>)}</SelectField></>
        )} />
        {sessions.isPending ? <LoadingState label="Loading attendance sessions" /> : sessions.isError ? <ErrorState error={sessions.error} onRetry={() => void sessions.refetch()} /> : !filtered.length ? <EmptyState title={sessions.data.length ? 'No sessions match these filters' : 'No attendance sessions yet'} description={sessions.data.length ? 'Try another course, faculty member or search phrase.' : 'Create a session for a course to begin taking attendance.'} /> : (
          <ResourceTable caption="Attendance sessions" columns={columns} rows={filtered} getRowKey={(session) => session.attendanceSessionId} actions={(session) => <Button size="sm" variant="ghost" title="View session and records" aria-label={`View session ${session.attendanceSessionId}`} onClick={() => setViewing(session)}><Eye size={16} /></Button>} />
        )}
      </Card>
      <AttendanceSessionFormDialog open={formOpen} onOpenChange={setFormOpen} courses={(courses.data ?? []).filter((course) => course.status === 'ACTIVE')} faculty={(faculty.data ?? []).filter((member) => member.status === 'ACTIVE')} onSubmit={save} />
      <AttendanceSessionDetailsDialog session={viewing} onOpenChange={(open) => { if (!open) setViewing(null) }} courses={courses.data ?? []} faculty={faculty.data ?? []} />
    </div>
  )
}
