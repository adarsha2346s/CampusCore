import { useQuery } from '@tanstack/react-query'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { PageHeader } from '../../components/ui/PageHeader'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { getMyAttendance, studentAttendanceKeys } from '../attendance/attendance.api'
import type { StudentAttendance } from '../attendance/attendance.api'

/**
 * The signed-in student's own attendance.
 *
 * One request, resolved entirely on the server: GET /api/v1/attendance-records/me
 * takes no identifier and returns this student's rows with the course and session
 * already joined in.
 *
 * The previous implementation assembled this from one request per enrollment and
 * then called GET /api/v1/attendance-sessions for the session names. That listing
 * is administrator-only, so the request was refused and every student saw an error
 * instead of their attendance. The student-scoped route replaces it rather than
 * widening the institution-wide one, which stays admin-only.
 */
export function StudentAttendancePage() {
  const attendance = useQuery({
    queryKey: studentAttendanceKeys.mine,
    queryFn: getMyAttendance,
  })

  const rows = attendance.data ?? []
  const present = rows.filter((row) => row.status === 'PRESENT').length
  const attendanceRate = rows.length === 0 ? null : Math.round((present / rows.length) * 100)

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="Academic"
        title="My attendance"
        description="Attendance recorded for your own enrollments, exactly as your faculty marked it."
      />

      {attendance.isPending && <LoadingState label="Loading your attendance" />}

      {attendance.isError && (
        <ErrorState error={attendance.error} onRetry={() => void attendance.refetch()} />
      )}

      {attendance.data?.length === 0 && (
        <EmptyState
          title="Nothing marked yet"
          description="Attendance appears here once your faculty mark a session for your enrollment."
        />
      )}

      {attendance.data && attendance.data.length > 0 && (
        <>
          <Card as="section">
            <dl className="metric-strip">
              <div className="metric-strip__item">
                <dt className="metric-strip__label">Records</dt>
                <dd className="metric-strip__value">{rows.length}</dd>
              </div>
              <div className="metric-strip__item">
                <dt className="metric-strip__label">Present</dt>
                <dd className="metric-strip__value">{present}</dd>
              </div>
              <div className="metric-strip__item">
                <dt className="metric-strip__label">Attendance rate</dt>
                <dd className="metric-strip__value">{attendanceRate}%</dd>
              </div>
            </dl>
          </Card>

          <Card>
            <ResourceTable
              caption="Attendance recorded for your enrollments"
              columns={[
                {
                  key: 'course',
                  header: 'Course',
                  render: (row: StudentAttendance) => (
                    <div className="identity-cell">
                      <span className="identity-avatar" aria-hidden="true">
                        {row.courseCode.slice(0, 1)}
                      </span>
                      <span>
                        <strong>{row.courseName}</strong>
                        <small>{row.courseCode}</small>
                      </span>
                    </div>
                  ),
                },
                {
                  key: 'session',
                  header: 'Session',
                  mobileHidden: true,
                  render: (row: StudentAttendance) => (
                    <span>
                      <strong>{row.topic || 'Session without a topic'}</strong>
                      <small className="cell-subtext">{row.sessionDate}</small>
                    </span>
                  ),
                },
                {
                  key: 'status',
                  header: 'Status',
                  render: (row: StudentAttendance) => <Badge>{row.status}</Badge>,
                },
              ]}
              rows={rows}
              getRowKey={(row) => row.attendanceRecordId}
            />
          </Card>
        </>
      )}
    </div>
  )
}