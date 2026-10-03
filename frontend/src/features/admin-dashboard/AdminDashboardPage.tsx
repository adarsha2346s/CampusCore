import { useQuery } from '@tanstack/react-query'
import { ArrowRight, BookOpen, GraduationCap, UserRound, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { PageHeader } from '../../components/ui/PageHeader'
import { ErrorState } from '../../components/data-display/ErrorState'
import { AnimatedNumber } from '../../components/data-display/AnimatedNumber'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { AnnouncementTicker } from '../../components/dashboard/AnnouncementTicker'
import { BentoCard } from '../../components/dashboard/BentoCard'
import { BarList } from '../../components/charts/BarList'
import { Heatmap } from '../../components/charts/Heatmap'
import { ProgressRing } from '../../components/charts/ProgressRing'
import { Rings } from '../../components/charts/Rings'
import { Sparkline } from '../../components/charts/Sparkline'
import { getAuditLogsPage, auditKeys } from '../audit/audit.api'
import { getAttendanceRecords, getAttendanceSessions, attendanceRecordKeys, attendanceSessionKeys } from '../attendance/attendance.api'
import { getAssessments, assessmentKeys } from '../assessments/assessments.api'
import { getCourses, courseKeys } from '../courses/courses.api'
import { getDepartments, departmentKeys } from '../departments/departments.api'
import { getEnrollments, enrollmentKeys } from '../enrollments/enrollments.api'
import { getFaculty, facultyKeys } from '../faculty/faculty.api'
import { getMarks, markKeys } from '../marks/marks.api'
import { getStudents, studentKeys } from '../students/students.api'
import { getUsers, userKeys } from '../users/users.api'
import {
  assessmentCoverage,
  buildAnnouncements,
  buildAttendanceHeatmap,
  enrolmentByCourse,
  share,
  studentAttendanceRates,
} from '../../lib/format/analytics'

const quickLinks = [
  { label: 'Manage students', to: '/admin/students', description: 'Review student profiles', icon: GraduationCap },
  { label: 'Manage faculty', to: '/admin/faculty', description: 'Browse faculty records', icon: UserRound },
  { label: 'Course catalog', to: '/admin/courses', description: 'Maintain academic offerings', icon: BookOpen },
  { label: 'User accounts', to: '/admin/users', description: 'Manage campus access', icon: Users },
]

export function AdminDashboardPage() {
  const users = useQuery({ queryKey: userKeys.all, queryFn: getUsers })
  const students = useQuery({ queryKey: studentKeys.all, queryFn: getStudents })
  const faculty = useQuery({ queryKey: facultyKeys.all, queryFn: getFaculty })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const courses = useQuery({ queryKey: courseKeys.all, queryFn: getCourses })
  const enrollments = useQuery({ queryKey: enrollmentKeys.all, queryFn: getEnrollments })
  const assessments = useQuery({ queryKey: assessmentKeys.all, queryFn: getAssessments })
  const marks = useQuery({ queryKey: markKeys.all, queryFn: getMarks })
  const sessions = useQuery({ queryKey: attendanceSessionKeys.all, queryFn: getAttendanceSessions })
  const records = useQuery({ queryKey: attendanceRecordKeys.all, queryFn: getAttendanceRecords })
  const audit = useQuery({ queryKey: auditKeys.page({ kind: 'all' }, 0, 5), queryFn: () => getAuditLogsPage({ kind: 'all' }, 0, 5) })

  const queries = [users, students, faculty, departments, courses, enrollments, assessments, marks, sessions, records, audit]

  if (queries.some((query) => query.isPending)) {
    return (
      <div className="admin-page admin-dashboard">
        <PageHeader eyebrow="Campus operations" title="Overview" description="A current view of the records available in CampusCore." />
        <section className="bento" aria-hidden="true">
          <div className="bento__cell bento__cell--big"><TableSkeleton rows={5} columns={4} /></div>
          {Array.from({ length: 6 }, (_, index) => (
            <div className={index === 3 ? 'bento__cell bento__cell--wide' : 'bento__cell'} key={index}>
              <div className="card dashboard-skeleton"><TableSkeleton rows={2} columns={1} /></div>
            </div>
          ))}
        </section>
        <span className="sr-only" role="status">Loading campus overview</span>
      </div>
    )
  }

  const failedQuery = queries.find((query) => query.isError)
  if (failedQuery) {
    return (
      <>
        <PageHeader eyebrow="Campus operations" title="Overview" description="A current view of the records available in CampusCore." />
        <ErrorState error={failedQuery.error} onRetry={() => { for (const query of queries) void query.refetch() }} />
      </>
    )
  }

  const studentList = students.data ?? []
  const facultyList = faculty.data ?? []
  const departmentList = departments.data ?? []
  const courseList = courses.data ?? []
  const enrollmentList = enrollments.data ?? []
  const assessmentList = assessments.data ?? []
  const markList = marks.data ?? []
  const sessionList = sessions.data ?? []
  const recordList = records.data ?? []
  const userTotal = users.data?.length ?? 0

  const activeStudents = studentList.filter((student) => student.status === 'ACTIVE').length
  const activeCourses = courseList.filter((course) => course.status === 'ACTIVE').length
  const auditEntries = audit.data?.totalElements ?? 0

  const heatmap = buildAttendanceHeatmap(sessionList, recordList)
  const rates = studentAttendanceRates(enrollmentList, recordList)
  const lowAttendance = studentList
    .map((student) => {
      const rate = rates.get(student.studentId)
      const department = departmentList.find((item) => item.departmentId === student.departmentId)
      return rate
        ? { student, percentage: rate.percentage, department: department?.name ?? 'Department unavailable' }
        : null
    })
    .filter((entry): entry is { student: typeof studentList[number]; percentage: number; department: string } => entry !== null && entry.percentage < 85)
    .sort((a, b) => a.percentage - b.percentage)
    .slice(0, 6)

  const enrolmentChart = enrolmentByCourse(enrollmentList, courseList)
  const coverage = assessmentCoverage(assessmentList, markList)
  const announcements = buildAnnouncements({
    auditLogs: audit.data?.content ?? [],
    assessments: assessmentList,
    sessions: sessionList,
    courses: courseList,
    studentTotal: studentList.length,
    facultyTotal: facultyList.length,
    enrollmentTotal: enrollmentList.length,
  })

  const hasRecords = userTotal + studentList.length + facultyList.length + departmentList.length + courseList.length > 0

  return (
    <div className="admin-page admin-dashboard">
      <PageHeader eyebrow="Campus operations" title="Overview" description="A current view of the records available in CampusCore." />
      <AnnouncementTicker items={announcements} />

      {!hasRecords && (
        <div className="setup-callout" role="status">
          <div>
            <strong>Your campus directory is ready to set up.</strong>
            <p>Start with a department, then add courses and link student and faculty profiles to user accounts.</p>
          </div>
          <Link className="button button--secondary button--md" to="/admin/departments">
            Add a department <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </div>
      )}

      <section className="bento" aria-label="Campus overview">
        <BentoCard size="big" index={0} title="Attendance, last four weeks" hint={`Darker days mean more students present. ${heatmap.windowLabel}.`}>
          {heatmap.cells.length === 0 ? (
            <p className="form-hint">No attendance sessions have been recorded yet. Create a session to start tracking presence.</p>
          ) : (
            <Heatmap cells={heatmap.cells} label="Attendance percentage per day over the last four weeks" caption="Fewer present" />
          )}
        </BentoCard>

        <BentoCard index={1}>
          <span className="kpi__value"><AnimatedNumber value={studentList.length} /></span>
          <p className="kpi__label">Students enrolled</p>
          <p className="kpi__detail">{activeStudents} active</p>
        </BentoCard>

        <BentoCard index={2}>
          <span className="kpi__value"><AnimatedNumber value={heatmap.overall ?? 0} suffix="%" /></span>
          <p className="kpi__label">Average attendance</p>
          {heatmap.trend.some((week) => week > 0) && (
            <Sparkline className="kpi__spark" values={heatmap.trend} label="Weekly attendance trend across the last four weeks" />
          )}
        </BentoCard>

        <BentoCard size="wide" index={3} title="Below 85% attendance" hint="Worth a conversation before the next assessment.">
          {lowAttendance.length === 0 ? (
            <p className="form-hint">No student is below the 85% attendance threshold in the current records.</p>
          ) : (
            <ul className="data-list">
              {lowAttendance.map(({ student, percentage, department }) => (
                <li className="data-list__row" key={student.studentId}>
                  <span className="data-list__copy">
                    <strong>{`${student.firstName} ${student.lastName ?? ''}`.trim()}</strong>
                    <small>{`${department} · ${student.enrollmentNumber}`}</small>
                  </span>
                  <span className="data-list__tag">{percentage}%</span>
                </li>
              ))}
            </ul>
          )}
        </BentoCard>

        <BentoCard index={4}>
          <span className="kpi__value"><AnimatedNumber value={courseList.length} /></span>
          <p className="kpi__label">Courses running</p>
          <p className="kpi__detail">{activeCourses} active</p>
        </BentoCard>

        <BentoCard index={5} highlighted>
          <span className="kpi__value"><AnimatedNumber value={auditEntries} /></span>
          <p className="kpi__label">Audit entries to review</p>
          <p className="kpi__detail">Recorded activity</p>
        </BentoCard>

        <BentoCard size="wide" index={6} title="Campus composition" hint="Share of active records across the directory.">
          <Rings layout="grid" label="Campus composition">
            <ProgressRing value={share(activeStudents, studentList.length) ?? 0} label="students" caption={`${activeStudents}/${studentList.length}`} />
            <ProgressRing value={share(activeCourses, courseList.length) ?? 0} label="courses" caption={`${activeCourses}/${courseList.length}`} />
            <ProgressRing value={(coverage ?? 0) / 100} label="assessed" caption={coverage === null ? 'n/a' : `${coverage}%`} />
          </Rings>
        </BentoCard>

        <BentoCard size="full" index={7} title="Enrolment by course" hint="Live enrollments grouped by catalog course, largest first.">
          {enrolmentChart.length === 0 ? (
            <p className="form-hint">No enrollments have been recorded yet.</p>
          ) : (
            <BarList items={enrolmentChart} label="Enrolments per course" caption="Enrollments with a DROPPED status are excluded." />
          )}
        </BentoCard>

        <BentoCard size="full" index={8} title="Common tasks" hint="Jump straight into the directories you maintain.">
          <div className="quick-links">
            {quickLinks.map(({ label, to, description, icon: Icon }) => (
              <Link className="quick-link" to={to} key={to} viewTransition>
                <span className="quick-link__icon"><Icon size={18} aria-hidden="true" /></span>
                <span className="quick-link__text"><strong>{label}</strong><small>{description}</small></span>
                <ArrowRight size={16} aria-hidden="true" />
              </Link>
            ))}
          </div>
        </BentoCard>
      </section>

      <p className="directory-footnote">
        Every figure on this page is derived from the CampusCore records currently available to your account.
      </p>
    </div>
  )
}