import type {
  AssessmentResponse,
  AttendanceRecordResponse,
  AttendanceSessionResponse,
  AuditLogResponse,
  CourseResponse,
  EnrollmentResponse,
  MarkResponse,
  StudentResponse,
} from '../../types/api'
import type { HeatCell } from '../../components/charts/Heatmap'

const DAY_MS = 86_400_000

function parseDay(value: string): Date | null {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return null
  return parsed
}

function isoDay(date: Date) {
  return date.toISOString().slice(0, 10)
}

export interface AttendanceTotals {
  attended: number
  total: number
}

/** PRESENT and LATE both count as attending a session. */
export function attendanceTotals(records: AttendanceRecordResponse[]): AttendanceTotals {
  let attended = 0
  let total = 0
  for (const record of records) {
    total += 1
    if (record.status === 'PRESENT' || record.status === 'LATE') attended += 1
  }
  return { attended, total }
}

export function toPercentage(totals: AttendanceTotals): number | null {
  if (totals.total === 0) return null
  return Math.round((totals.attended / totals.total) * 100)
}

export interface AttendanceHeatmap {
  cells: HeatCell[]
  overall: number | null
  trend: number[]
  windowLabel: string
}

/**
 * Builds the "last N weeks" attendance grid used by the dashboard. Days without
 * a recorded session stay blank instead of being reported as zero attendance.
 */
export function buildAttendanceHeatmap(
  sessions: AttendanceSessionResponse[],
  records: AttendanceRecordResponse[],
  weeks = 4,
): AttendanceHeatmap {
  const perSession = new Map<number, AttendanceRecordResponse[]>()
  for (const record of records) {
    const bucket = perSession.get(record.attendanceSessionId)
    if (bucket) bucket.push(record)
    else perSession.set(record.attendanceSessionId, [record])
  }

  const perDay = new Map<string, AttendanceTotals>()
  let latest = 0
  for (const session of sessions) {
    const day = parseDay(session.sessionDate)
    if (!day) continue
    const key = isoDay(day)
    latest = Math.max(latest, day.getTime())
    const totals = attendanceTotals(perSession.get(session.attendanceSessionId) ?? [])
    const existing = perDay.get(key) ?? { attended: 0, total: 0 }
    perDay.set(key, { attended: existing.attended + totals.attended, total: existing.total + totals.total })
  }

  const overallTotals = [...perDay.values()].reduce<AttendanceTotals>(
    (sum, totals) => ({ attended: sum.attended + totals.attended, total: sum.total + totals.total }),
    { attended: 0, total: 0 },
  )

  if (!latest) {
    return { cells: [], overall: null, trend: [], windowLabel: 'No attendance sessions recorded yet' }
  }

  // Align the window to whole Monday–Sunday weeks ending with the latest session.
  const latestDate = new Date(latest)
  const endOfWeek = new Date(latestDate.getTime() - ((latestDate.getUTCDay() + 6) % 7) * DAY_MS)
  const start = new Date(endOfWeek.getTime() - (weeks - 1) * 7 * DAY_MS)

  const cells: HeatCell[] = []
  const trend: number[] = []
  for (let week = 0; week < weeks; week += 1) {
    let weekAttended = 0
    let weekTotal = 0
    for (let day = 0; day < 7; day += 1) {
      const date = new Date(start.getTime() + (week * 7 + day) * DAY_MS)
      const key = isoDay(date)
      const totals = perDay.get(key)
      if (!totals || totals.total === 0) {
        const future = date.getTime() > latest
        cells.push({
          value: null,
          title: `${date.toLocaleDateString(undefined, { dateStyle: 'medium' })} · ${future ? 'Upcoming' : 'No classes recorded'}`,
        })
        continue
      }
      weekAttended += totals.attended
      weekTotal += totals.total
      const percentage = Math.round((totals.attended / totals.total) * 100)
      cells.push({
        value: percentage,
        title: `${date.toLocaleDateString(undefined, { dateStyle: 'medium' })} · ${percentage}% present (${totals.attended}/${totals.total})`,
      })
    }
    trend.push(weekTotal === 0 ? 0 : Math.round((weekAttended / weekTotal) * 100))
  }

  const windowLabel = `${start.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} – ${new Date(endOfWeek.getTime() + 6 * DAY_MS).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}`

  return { cells, overall: toPercentage(overallTotals), trend, windowLabel }
}

export interface StudentAttendance {
  studentId: number
  percentage: number
  attended: number
  total: number
}

/** Attendance rate per student, resolved through their enrollments. */
export function studentAttendanceRates(
  enrollments: EnrollmentResponse[],
  records: AttendanceRecordResponse[],
): Map<number, StudentAttendance> {
  const perEnrollment = new Map<number, AttendanceTotals>()
  for (const record of records) {
    const totals = perEnrollment.get(record.enrollmentId) ?? { attended: 0, total: 0 }
    totals.total += 1
    if (record.status === 'PRESENT' || record.status === 'LATE') totals.attended += 1
    perEnrollment.set(record.enrollmentId, totals)
  }

  const perStudent = new Map<number, StudentAttendance>()
  for (const enrollment of enrollments) {
    const totals = perEnrollment.get(enrollment.enrollmentId)
    if (!totals) continue
    const existing = perStudent.get(enrollment.studentId) ?? { studentId: enrollment.studentId, percentage: 0, attended: 0, total: 0 }
    existing.attended += totals.attended
    existing.total += totals.total
    perStudent.set(enrollment.studentId, existing)
  }
  for (const entry of perStudent.values()) {
    entry.percentage = entry.total === 0 ? 0 : Math.round((entry.attended / entry.total) * 100)
  }
  return perStudent
}

export interface EnrolmentDatum {
  label: string
  meta?: string
  value: number
}

/** Enrolments per course, largest first. */
export function enrolmentByCourse(enrollments: EnrollmentResponse[], courses: CourseResponse[], limit = 6): EnrolmentDatum[] {
  const counts = new Map<number, number>()
  for (const enrollment of enrollments) {
    if (enrollment.status === 'DROPPED') continue
    counts.set(enrollment.courseId, (counts.get(enrollment.courseId) ?? 0) + 1)
  }
  return [...counts.entries()]
    .map(([courseId, value]) => {
      const course = courses.find((item) => item.courseId === courseId)
      return { label: course?.courseName ?? `Course #${courseId}`, meta: course?.courseCode, value }
    })
    .sort((a, b) => b.value - a.value)
    .slice(0, limit)
}

export interface DepartmentDatum {
  departmentId: number
  name: string
  code: string
  students: number
}

/** Student headcount per department for the faculty directory view. */
export function studentsByDepartment(students: StudentResponse[], departments: { departmentId: number; name: string; code: string }[]): DepartmentDatum[] {
  const counts = new Map<number, number>()
  for (const student of students) {
    counts.set(student.departmentId, (counts.get(student.departmentId) ?? 0) + 1)
  }
  return departments
    .map((department) => ({ ...department, students: counts.get(department.departmentId) ?? 0 }))
    .sort((a, b) => b.students - a.students)
}

/** Share of assessments that already have at least one mark recorded. */
export function assessmentCoverage(assessments: AssessmentResponse[], marks: MarkResponse[]): number | null {
  if (assessments.length === 0) return null
  const assessed = new Set(marks.map((mark) => mark.assessmentId))
  const covered = assessments.filter((assessment) => assessed.has(assessment.assessmentId)).length
  return Math.round((covered / assessments.length) * 100)
}

function ratio(numerator: number, denominator: number): number | null {
  return denominator === 0 ? null : numerator / denominator
}

function formatDate(value: string) {
  const parsed = parseDay(value)
  return parsed ? parsed.toLocaleDateString(undefined, { dateStyle: 'medium' }) : value
}

/**
 * Campus notices derived strictly from recorded data: audit activity, upcoming
 * assessments, recent sessions and current directory counts.
 */
export function buildAnnouncements(input: {
  auditLogs: AuditLogResponse[]
  assessments: AssessmentResponse[]
  sessions: AttendanceSessionResponse[]
  courses: CourseResponse[]
  studentTotal: number
  facultyTotal: number
  enrollmentTotal: number
}): string[] {
  const items: string[] = []

  const upcoming = input.assessments
    .filter((assessment) => assessment.assessmentDate)
    .map((assessment) => ({ assessment, time: parseDay(assessment.assessmentDate as string)?.getTime() ?? 0 }))
    .filter((entry) => entry.time >= Date.now() - 86_400_000)
    .sort((a, b) => a.time - b.time)
    .slice(0, 2)
  for (const { assessment } of upcoming) {
    const course = input.courses.find((item) => item.courseId === assessment.courseId)
    items.push(`${assessment.name}${course ? ` · ${course.courseCode}` : ''} is scheduled for ${formatDate(assessment.assessmentDate as string)}`)
  }

  const recentSessions = [...input.sessions]
    .map((session) => ({ session, time: parseDay(session.sessionDate)?.getTime() ?? 0 }))
    .sort((a, b) => b.time - a.time)
    .slice(0, 2)
  for (const { session } of recentSessions) {
    const course = input.courses.find((item) => item.courseId === session.courseId)
    items.push(`Attendance recorded for ${course?.courseCode ?? `course #${session.courseId}`} on ${formatDate(session.sessionDate)}`)
  }

  for (const log of input.auditLogs.slice(0, 2)) {
    items.push(`${log.action.toLowerCase()} recorded on ${log.entityName.toLowerCase()}${log.createdAt ? ` · ${formatDate(log.createdAt)}` : ''}`)
  }

  items.push(`${input.studentTotal.toLocaleString()} student profiles in the directory · ${input.facultyTotal.toLocaleString()} faculty profiles`)
  items.push(`${input.enrollmentTotal.toLocaleString()} course enrollments on record`)

  return items
}

/** Percentage helper used by the dashboard rings. */
export function share(part: number, whole: number): number | null {
  return ratio(part, whole)
}