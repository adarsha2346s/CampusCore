import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Drawer } from '../ui/Drawer'
import { ErrorState } from '../data-display/ErrorState'
import { LoadingState } from '../data-display/LoadingState'
import { StatusBadge } from '../../features/admin-shared/StatusBadge'
import { AttendanceBar } from '../charts/AttendanceBar'
import { getAttendanceRecords, attendanceRecordKeys } from '../../features/attendance/attendance.api'
import { getCourses, courseKeys } from '../../features/courses/courses.api'
import { getEnrollments, enrollmentKeys } from '../../features/enrollments/enrollments.api'
import { getUsers, userKeys } from '../../features/users/users.api'
import { attendanceTotals, toPercentage } from '../../lib/format/analytics'
import { formatDate } from '../../lib/format/datetime'
import type { StudentResponse } from '../../types/api'

interface Student360DrawerProps {
  student: StudentResponse | null
  onOpenChange: (open: boolean) => void
  departmentName?: string
}

/**
 * Student 360 slide-over: identity, department placement, enrolled courses and
 * the attendance recorded against those enrollments.
 */
export function Student360Drawer({ student, onOpenChange, departmentName }: Student360DrawerProps) {
  const open = student !== null
  const enrollments = useQuery({ queryKey: enrollmentKeys.all, queryFn: getEnrollments, enabled: open })
  const records = useQuery({ queryKey: attendanceRecordKeys.all, queryFn: getAttendanceRecords, enabled: open })
  const courses = useQuery({ queryKey: courseKeys.all, queryFn: getCourses, enabled: open })
  const users = useQuery({ queryKey: userKeys.all, queryFn: getUsers, enabled: open })

  const detail = useMemo(() => {
    if (!student) return null
    const own = (enrollments.data ?? []).filter((enrollment) => enrollment.studentId === student.studentId)
    const totals = attendanceTotals((records.data ?? []).filter((record) => own.some((enrollment) => enrollment.enrollmentId === record.enrollmentId)))
    const rows = own.map((enrollment) => {
      const course = (courses.data ?? []).find((item) => item.courseId === enrollment.courseId)
      const courseRecords = (records.data ?? []).filter((record) => record.enrollmentId === enrollment.enrollmentId)
      return {
        enrollment,
        courseName: course?.courseName ?? `Course #${enrollment.courseId}`,
        courseCode: course?.courseCode ?? '—',
        credits: course?.credits ?? null,
        attendance: toPercentage(attendanceTotals(courseRecords)),
      }
    })
    const account = (users.data ?? []).find((user) => user.userId === student.userId)
    return { rows, totals, account, attendance: toPercentage(totals) }
  }, [student, enrollments.data, records.data, courses.data, users.data])

  const loading = enrollments.isPending || records.isPending || courses.isPending
  const failed = enrollments.error ?? records.error ?? courses.error

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
          <div><dt>Account</dt><dd>{detail?.account?.username ?? `User #${student.userId}`}</dd></div>
          <div><dt>Date of birth</dt><dd>{formatDate(student.dateOfBirth) ?? <span className="muted">Not provided</span>}</dd></div>
          <div><dt>Phone</dt><dd>{student.phone ?? <span className="muted">Not provided</span>}</dd></div>
        </dl>

        <h3 className="drawer-section-title">Attendance</h3>
        {loading ? <LoadingState label="Loading attendance" />
          : failed ? <ErrorState error={failed} onRetry={() => { void enrollments.refetch(); void records.refetch() }} />
            : <p className="drawer-metric">
              <AttendanceBar value={detail?.attendance ?? null} />
              <span className="drawer-metric__label">
                {detail && detail.totals.total > 0
                  ? `${detail.totals.attended} of ${detail.totals.total} recorded attendances`
                  : 'No attendance has been recorded for this student'}
              </span>
            </p>}

        <h3 className="drawer-section-title">Enrolled courses</h3>
        {loading ? <LoadingState label="Loading enrollments" />
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