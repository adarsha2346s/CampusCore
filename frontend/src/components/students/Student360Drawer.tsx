import { useMemo } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { ShieldAlert } from 'lucide-react'
import { Drawer } from '../ui/Drawer'
import { ErrorState } from '../data-display/ErrorState'
import { LoadingState } from '../data-display/LoadingState'
import { StatusBadge } from '../../features/admin-shared/StatusBadge'
import { AttendanceBar } from '../charts/AttendanceBar'
import { getAttendanceRecordsByEnrollment, attendanceRecordKeys } from '../../features/attendance/attendance.api'
import { getCourses, courseKeys } from '../../features/courses/courses.api'
import { getStudentEnrollments, studentKeys } from '../../features/students/students.api'
import { getUser } from '../../features/users/users.api'
import { useAuth } from '../../features/auth/auth-context'
import { attendanceTotals, toPercentage } from '../../lib/format/analytics'
import { formatDate } from '../../lib/format/datetime'
import { ApiError } from '../../lib/api/api-error'
import type { AttendanceRecordResponse } from '../../types/api'
import type { StudentResponse } from '../../types/api'

interface Student360DrawerProps {
  student: StudentResponse | null
  onOpenChange: (open: boolean) => void
  departmentName?: string
}

/**
 * Student 360 slide-over: identity, department placement, enrolled courses and
 * the attendance recorded against those enrollments.
 *
 * <p>The data comes from endpoints the caller is actually allowed to use. The
 * administrator-only collections are not read: a faculty member cannot call
 * them, and asking for them would answer 403, which is a refusal rather than
 * data. Instead the enrollments come from the student's own route, which the
 * server narrows to the courses the caller teaches, and the attendance comes
 * from the enrollment-scoped route.
 *
 * <p>The identity is the record the page already holds, so it needs no request
 * of its own.
 */
export function Student360Drawer({ student, onOpenChange, departmentName }: Student360DrawerProps) {
  const open = student !== null
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const enrollments = useQuery({
    queryKey: studentKeys.enrollments(student?.studentId ?? 0),
    queryFn: () => getStudentEnrollments(student?.studentId as number),
    enabled: open,
  })

  const courses = useQuery({
    queryKey: courseKeys.all,
    queryFn: getCourses,
    enabled: open,
  })

  // The account behind the student is administrator-only, so it is read only
  // when the caller may read it. For anyone else the field says so rather than
  // provoking a refusal.
  const account = useQuery({
    queryKey: ['admin', 'users', student?.userId ?? 0],
    queryFn: () => getUser(student?.userId as number),
    enabled: open && isAdmin,
  })

  /**
   * Attendance is recorded against an enrollment, so it is read per enrollment.
   * Each query is keyed by the enrollment it belongs to, so opening the drawer
   * again reuses what was already fetched.
   */
  const attendanceQueries = useQueries({
    queries: (enrollments.data ?? []).map((enrollment) => ({
      queryKey: attendanceRecordKeys.byEnrollment(String(enrollment.enrollmentId)),
      queryFn: () => getAttendanceRecordsByEnrollment(enrollment.enrollmentId),
      enabled: open,
    })),
  })

  const detail = useMemo(() => {
    if (!student) return null
    const rows = (enrollments.data ?? []).map((enrollment, index) => {
      const course = (courses.data ?? []).find((item) => item.courseId === enrollment.courseId)
      const records = (attendanceQueries[index]?.data ?? []) as AttendanceRecordResponse[]
      const totals = attendanceTotals(records)
      return {
        enrollment,
        courseName: course?.courseName ?? `Course #${enrollment.courseId}`,
        courseCode: course?.courseCode ?? '—',
        credits: course?.credits ?? null,
        attendance: toPercentage(totals),
        totals,
      }
    })
    const totals = attendanceTotals(
      attendanceQueries.flatMap((query) => (query.data ?? []) as AttendanceRecordResponse[]),
    )
    return { rows, totals, attendance: toPercentage(totals), account: account.data }
  }, [student, enrollments.data, courses.data, attendanceQueries, account.data])

  const loading = enrollments.isPending || courses.isPending
  const failed = enrollments.error ?? courses.error
  const permissionDenied = failed instanceof ApiError && failed.status === 403
  const attendanceFailed = attendanceQueries.some((query) => query.isError)

  return (
    <Drawer
      open={open}
      onOpenChange={onOpenChange}
      title={student ? `${student.firstName} ${student.lastName ?? ''}`.trim() : 'Student details'}
      description={student ? `${student.enrollmentNumber} · ${departmentName ?? 'Department unavailable'}` : undefined}
    >
      {!student ? null : <>
        <div className="drawer-identity">
          <span className="identity-avatar" aria-hidden="true">{student.firstName.slice(0, 1).toUpperCase()}</span>
          <div>
            <StatusBadge status={student.status} />
            <p className="form-hint">Student ID #{student.studentId}</p>
          </div>
        </div>

        <dl className="record-details">
          <div><dt>Enrollment number</dt><dd className="mono-label">{student.enrollmentNumber}</dd></div>
          <div><dt>Department</dt><dd>{departmentName ?? `Department #${student.departmentId}`}</dd></div>
          <div><dt>Admission year</dt><dd>{student.admissionYear}</dd></div>
          <div>
            <dt>Account</dt>
            <dd>
              {isAdmin
                ? account.isPending
                  ? 'Loading…'
                  : account.data?.username ?? `User #${student.userId}`
                : 'Not available for your role'}
            </dd>
          </div>
          <div><dt>Date of birth</dt><dd>{formatDate(student.dateOfBirth) ?? <span className="muted">Not provided</span>}</dd></div>
          <div><dt>Phone</dt><dd>{student.phone ?? <span className="muted">Not provided</span>}</dd></div>
        </dl>

        <h3 className="drawer-section-title">Attendance</h3>
        {loading ? <LoadingState label="Loading attendance" />
          : failed ? (
            permissionDenied ? (
              <div className="state-panel state-panel--error" role="alert">
                <span className="state-panel__icon"><ShieldAlert size={22} aria-hidden="true" /></span>
                <h2>This student is outside your scope</h2>
                <p>You can only open the records of a student you teach. Ask an administrator if you believe this is wrong.</p>
              </div>
            ) : (
              <ErrorState error={failed} onRetry={() => { void enrollments.refetch(); void courses.refetch() }} />
            )
          ) : attendanceFailed ? (
            <ErrorState
              error={attendanceQueries.find((query) => query.isError)?.error}
              onRetry={() => attendanceQueries.forEach((query) => void query.refetch())}
            />
          ) : (
            <p className="drawer-metric">
              <AttendanceBar value={detail?.attendance ?? null} />
              <span className="drawer-metric__label">
                {detail && detail.totals.total > 0
                  ? `${detail.totals.attended} of ${detail.totals.total} recorded attendances`
                  : 'No attendance has been recorded for this student'}
              </span>
            </p>
          )}

        <h3 className="drawer-section-title">Enrolled courses</h3>
        {loading ? <LoadingState label="Loading enrollments" />
          : failed ? null
          : detail && detail.rows.length > 0 ? (
            <ul className="data-list">
              {detail.rows.map((row) => (
                <li className="data-list__row" key={row.enrollment.enrollmentId}>
                  <span className="data-list__copy">
                    <strong>{row.courseName}</strong>
                    <small>{row.courseCode} · {row.enrollment.semester} {row.enrollment.academicYear}</small>
                  </span>
                  <span className="data-list__tag data-list__tag--value">
                    {row.attendance === null ? '—' : `${row.attendance}%`}
                  </span>
                </li>
              ))}
            </ul>
          ) : <p className="form-hint">No course enrollments are linked to this student profile.</p>}
      </>}
    </Drawer>
  )
}
