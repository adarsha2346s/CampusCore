import { useQuery } from '@tanstack/react-query'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { Dialog } from '../../components/ui/Dialog'
import { attendanceRecordKeys, attendanceSessionKeys, getAttendanceRecordsBySession, getAttendanceSession } from './attendance.api'
import type { AttendanceRecordResponse, AttendanceSessionResponse, CourseResponse, FacultyResponse } from '../../types/api'

export function AttendanceSessionDetailsDialog({ session, onOpenChange, courses, faculty }: {
  session: AttendanceSessionResponse | null
  onOpenChange: (open: boolean) => void
  courses: CourseResponse[]
  faculty: FacultyResponse[]
}) {
  const sessionId = session?.attendanceSessionId
  const detail = useQuery({ queryKey: attendanceSessionKeys.detail(sessionId ?? 0), queryFn: () => getAttendanceSession(sessionId!), enabled: sessionId !== undefined })
  const records = useQuery({ queryKey: attendanceRecordKeys.bySession(String(sessionId ?? '')), queryFn: () => getAttendanceRecordsBySession(sessionId!), enabled: sessionId !== undefined })
  const current = detail.data ?? session
  const course = courses.find((item) => item.courseId === current?.courseId)
  const member = faculty.find((item) => item.facultyId === current?.facultyId)
  const columns = [
    { key: 'id', header: 'Record', render: (record: AttendanceRecordResponse) => `#${record.attendanceRecordId}` },
    { key: 'enrollment', header: 'Enrollment', render: (record: AttendanceRecordResponse) => `#${record.enrollmentId}` },
    { key: 'status', header: 'Status', render: (record: AttendanceRecordResponse) => <Badge className={`attendance-status attendance-status--${record.status.toLowerCase()}`}>{record.status}</Badge> },
  ]
  return (
    <Dialog open={session !== null} onOpenChange={onOpenChange} title={current?.topic || `Session #${sessionId}`} description="Attendance session and its individual records.">
      {detail.isError ? <ErrorState error={detail.error} onRetry={() => void detail.refetch()} /> : detail.isPending ? <LoadingState label="Loading session details" /> : (
        <div className="session-detail-stack">
          <dl className="record-details"><div><dt>Course</dt><dd>{course ? `${course.courseCode} · ${course.courseName}` : `Course #${current?.courseId}`}</dd></div><div><dt>Faculty</dt><dd>{member ? `${member.firstName} ${member.lastName ?? ''} · ${member.employeeNumber}` : `Faculty #${current?.facultyId}`}</dd></div><div><dt>Session date</dt><dd>{current?.sessionDate}</dd></div><div><dt>Topic</dt><dd>{current?.topic || <span className="muted">Not provided</span>}</dd></div></dl>
          <Card className="session-records-card"><div className="section-heading"><div><p className="eyebrow">Session activity</p><h2>Attendance records</h2></div>{records.data && <Badge>{records.data.length} records</Badge>}</div>
            {records.isPending ? <LoadingState label="Loading session records" /> : records.isError ? <ErrorState error={records.error} onRetry={() => void records.refetch()} /> : !records.data.length ? <EmptyState title="No attendance records yet" description="Individual attendance records will appear here as they are entered." /> : <ResourceTable caption="Attendance records for this session" columns={columns} rows={records.data} getRowKey={(record) => record.attendanceRecordId} />}
          </Card>
        </div>
      )}
    </Dialog>
  )
}
