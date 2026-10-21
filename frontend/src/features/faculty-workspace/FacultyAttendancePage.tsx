import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card } from '../../components/ui/Card'
import { Badge } from '../../components/ui/Badge'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { PageHeader } from '../../components/ui/PageHeader'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { SelectField } from '../../components/forms/SelectField'
import { MyCoursePicker } from './MyCoursePicker'
import { facultyTeachingKeys, useMyCourses } from './useMyCourses'
import {
  getAttendanceRecordsBySession,
  getAttendanceSessionsByCourse,
} from '../attendance/attendance.api'
import { getEnrollmentsByCourse } from '../enrollments/enrollments.api'
import type { AttendanceRecordResponse, EnrollmentResponse } from '../../types/api'

/**
 * An attendance record joined to its enrollment.
 *
 * `enrollment` is optional because an attendance row only carries an enrollment
 * id. When the enrollment cannot be resolved the row is still shown, labelled by
 * its id, rather than being hidden or padded with invented student data.
 */
type ScopedRecord = {
  record: AttendanceRecordResponse
  enrollment: EnrollmentResponse | undefined
}

/**
 * Attendance for a course the signed-in faculty member teaches.
 *
 * Reads GET /api/v1/attendance-sessions/course/{courseId} then
 * GET /api/v1/attendance-records/session/{sessionId}. Both are authorised by the
 * course assignment server-side, so a session belonging to another faculty
 * member's course cannot be read through this page.
 */
export function FacultyAttendancePage() {
  const { courses, isPending, isError, error, refetch } = useMyCourses()
  const [courseId, setCourseId] = useState<number | null>(null)
  const [sessionId, setSessionId] = useState<number | null>(null)

  const activeCourseId = courseId ?? courses[0]?.courseId ?? null

  const sessions = useQuery({
    queryKey: facultyTeachingKeys.sessions(activeCourseId ?? 0),
    queryFn: () => getAttendanceSessionsByCourse(activeCourseId as number),
    enabled: activeCourseId !== null,
  })

  const activeSessionId = sessionId ?? sessions.data?.[0]?.attendanceSessionId ?? null

  const records = useQuery({
    queryKey: facultyTeachingKeys.records(activeSessionId ?? 0),
    queryFn: () => getAttendanceRecordsBySession(activeSessionId as number),
    enabled: activeSessionId !== null,
  })

  const enrollments = useQuery({
    queryKey: facultyTeachingKeys.enrollments(activeCourseId ?? 0),
    queryFn: () => getEnrollmentsByCourse(activeCourseId as number),
    enabled: activeCourseId !== null,
  })

  const enrollmentById = useMemo(
    () => new Map((enrollments.data ?? []).map((e) => [e.enrollmentId, e])),
    [enrollments.data],
  )

  const rows = useMemo<ScopedRecord[]>(
    () =>
      (records.data ?? []).map((record) => ({
        record,
        enrollment: enrollmentById.get(record.enrollmentId),
      })),
    [records.data, enrollmentById],
  )

  const studentLabel = (row: ScopedRecord) => {
    const enrollment = row.enrollment
    if (!enrollment) return `Enrollment #${row.record.enrollmentId}`
    return (
      `${enrollment.studentFirstName} ${enrollment.studentLastName ?? ''}`.trim() ||
      enrollment.enrollmentNumber ||
      `Enrollment #${row.record.enrollmentId}`
    )
  }

  const present = rows.filter((row) => row.record.status === 'PRESENT').length
  const rate = rows.length === 0 ? null : Math.round((present / rows.length) * 100)

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="Teaching"
        title="Attendance"
        description="Sessions and attendance for the courses you are assigned to teach."
      />

      <Card as="section">
        <MyCoursePicker
          selectedCourseId={activeCourseId}
          onSelect={(next) => {
            setCourseId(next)
            setSessionId(null)
          }}
          scopingHint="Sessions are scoped to the selected assigned course."
        />

        {sessions.isPending && (
          <p className="faculty-scope__hint" role="status">
            Loading sessions…
          </p>
        )}

        {sessions.isError && (
          <ErrorState error={sessions.error} onRetry={() => void sessions.refetch()} />
        )}

        {sessions.data && sessions.data.length > 0 && (
          <SelectField
            id="faculty-session"
            name="faculty-session"
            label="Session"
            value={String(activeSessionId)}
            onChange={(event) => setSessionId(Number(event.target.value))}
          >
            {sessions.data.map((session) => (
              <option key={session.attendanceSessionId} value={session.attendanceSessionId}>
                {new Date(session.sessionDate).toLocaleDateString()}
                {session.topic ? ` — ${session.topic}` : ''}
              </option>
            ))}
          </SelectField>
        )}

        {sessions.data && sessions.data.length === 0 && (
          <EmptyState
            title="No sessions recorded"
            description="Nobody has opened an attendance session for this course yet."
          />
        )}
      </Card>

      {isPending && <p className="faculty-scope__hint">Loading your courses…</p>}

      {isError && <ErrorState error={error} onRetry={refetch} />}

      {activeSessionId !== null && records.isPending && (
        <p className="faculty-scope__hint" role="status">
          Loading attendance records…
        </p>
      )}

      {records.isError && (
        <ErrorState error={records.error} onRetry={() => void records.refetch()} />
      )}

      {records.data && rows.length > 0 && (
        <>
          {rate !== null && (
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
                  <dd className="metric-strip__value">{rate}%</dd>
                </div>
              </dl>
            </Card>
          )}

          <Card>
            <ResourceTable
              caption="Attendance for the selected session"
              columns={[
                {
                  key: 'student',
                  header: 'Student',
                  render: (row: ScopedRecord) => (
                    <div className="identity-cell">
                      <span className="identity-avatar" aria-hidden="true">
                        {(row.enrollment?.studentFirstName ?? '#').slice(0, 1)}
                      </span>
                      <span>
                        <strong>{studentLabel(row)}</strong>
                        <small>
                          {row.enrollment
                            ? `${row.enrollment.enrollmentNumber} · ${row.enrollment.semester} ${row.enrollment.academicYear}`
                            : 'Enrollment details unavailable'}
                        </small>
                      </span>
                    </div>
                  ),
                },
                {
                  key: 'status',
                  header: 'Status',
                  render: (row: ScopedRecord) => <Badge>{row.record.status}</Badge>,
                },
              ]}
              rows={rows}
              getRowKey={(row) => row.record.attendanceRecordId}
            />
          </Card>
        </>
      )}

      {records.data && rows.length === 0 && (
        <EmptyState
          title="Nothing marked yet"
          description="Attendance records appear here once students are marked for this session."
        />
      )}
    </div>
  )
}
