import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, BookOpen, GraduationCap } from 'lucide-react'
import { ApiError } from '../../lib/api/api-error'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { AnimatedNumber } from '../../components/data-display/AnimatedNumber'
import { PageHeader } from '../../components/ui/PageHeader'
import { MetricStrip } from '../../components/charts/MetricStrip'
import { ProgressRing } from '../../components/charts/ProgressRing'
import { Rings } from '../../components/charts/Rings'
import type { StudentCourseSummary, StudentDashboardResponse } from '../../types/api'
import { getMyStudentProfile, studentKeys } from '../students/students.api'
import { StudentIdentityNotice } from './StudentIdentityNotice'
import { getStudentDashboard, studentDashboardKeys } from './student-workspace.api'

function fullName(firstName: string, lastName: string | null) {
  return [firstName, lastName].filter(Boolean).join(' ')
}

export function StudentOverviewPage() {
  const profile = useQuery({ queryKey: studentKeys.self, queryFn: getMyStudentProfile })
  const dashboard = useQuery({
    queryKey: studentDashboardKeys.detail(profile.data?.studentId ?? 0),
    queryFn: () => getStudentDashboard(profile.data!.studentId),
    enabled: profile.isSuccess,
  })

  const data: StudentDashboardResponse | undefined = dashboard.data
  const courses = data?.courses ?? []
  const gpa = data?.gpa ?? 0
  const clampedGpa = Math.min(10, Math.max(0, gpa))
  const averageAttendance = courses.length
    ? courses.reduce((sum, course) => sum + course.attendancePercentage, 0) / courses.length
    : 0
  const credits = courses.reduce((sum, course) => sum + course.credits, 0)

  return (
    <div className="role-workspace-page">
      <PageHeader eyebrow="Student portal" title="Overview" description="Your personal academic information from the campus records." />
      {profile.isPending ? <LoadingState label="Loading your student profile" />
        : profile.isError ? profile.error instanceof ApiError && profile.error.status === 404
          ? <StudentIdentityNotice section="dashboard" />
          : <ErrorState error={profile.error} onRetry={() => void profile.refetch()} />
          : <>
            <Card className="identity-card">
              <span className="identity-card__icon"><GraduationCap size={21} aria-hidden="true" /></span>
              <div>
                <p className="eyebrow">Student record</p>
                <h2>{fullName(profile.data.firstName, profile.data.lastName)}</h2>
                <p>{profile.data.enrollmentNumber} · {profile.data.departmentName}</p>
              </div>
              <Badge>{profile.data.status}</Badge>
            </Card>

            {dashboard.isPending ? <LoadingState label="Loading your academic summary" />
              : dashboard.isError ? <ErrorState error={dashboard.error} onRetry={() => void dashboard.refetch()} />
                : courses.length === 0 ? <EmptyState title="No course enrollments yet" description="Your student profile is linked, but the academic system has no course enrollments to include in your dashboard." />
                  : <StudentProgress
                      gpa={gpa}
                      clampedGpa={clampedGpa}
                      courses={courses}
                      alerts={data?.alerts ?? []}
                      averageAttendance={averageAttendance}
                      credits={credits}
                      enrollmentNumber={data?.enrollmentNumber}
                    />}
          </>}
    </div>
  )
}

function StudentProgress({ gpa, clampedGpa, courses, alerts, averageAttendance, credits, enrollmentNumber }: {
  gpa: number
  clampedGpa: number
  courses: StudentCourseSummary[]
  alerts: string[]
  averageAttendance: number
  credits: number
  enrollmentNumber?: string
}) {
  const lowest = courses.reduce((worst, course) => (course.attendancePercentage < worst.attendancePercentage ? course : worst))

  return (
    <>
      <MetricStrip
        label="Academic summary"
        items={[
          { label: 'Cumulative GPA', value: <AnimatedNumber value={clampedGpa} precision={2} />, detail: 'Out of 10.00' },
          { label: 'Average attendance', value: <AnimatedNumber value={averageAttendance} precision={1} suffix="%" />, detail: `Across ${courses.length} courses` },
          { label: 'Enrolled courses', value: <AnimatedNumber value={courses.length} />, detail: enrollmentNumber },
          { label: 'Registered credits', value: <AnimatedNumber value={credits} />, detail: 'Current term' },
        ]}
      />

      <Card className="bento-card" style={{ '--i': 0 } as React.CSSProperties}>
        <h2 className="bento-card__title">Progress rings</h2>
        <p className="bento-card__hint">Where your term currently stands, calculated from your academic records.</p>
        <Rings label="Academic progress rings">
          <ProgressRing value={clampedGpa / 10} label="term GPA" caption={gpa.toFixed(2)} />
          <ProgressRing value={averageAttendance / 100} label="attendance" caption={`${Math.round(averageAttendance)}%`} />
          <ProgressRing value={lowest.attendancePercentage / 100} label={`lowest · ${lowest.courseCode}`} caption={`${Math.round(lowest.attendancePercentage)}%`} />
        </Rings>
      </Card>

      <section aria-labelledby="student-courses-title">
        <div className="role-workspace-section-heading">
          <p className="eyebrow">Academic record</p>
          <h2 id="student-courses-title">My courses</h2>
        </div>
        <div className="student-course-grid">
          {courses.map((course) => (
            <Card className="student-course-card" key={course.courseId}>
              <div className="student-course-card__heading">
                <span className="entity-icon"><BookOpen size={17} aria-hidden="true" /></span>
                <Badge>{course.credits} credits</Badge>
              </div>
              <p className="eyebrow">{course.courseCode}</p>
              <h3>{course.courseName}</h3>
              <div className="student-course-card__attendance">
                <span>Attendance</span>
                <strong>{Math.round(course.attendancePercentage)}%</strong>
              </div>
              <div
                className="student-course-card__attendance-track"
                role="progressbar"
                aria-label={`${course.courseName} attendance`}
                aria-valuenow={Math.round(course.attendancePercentage)}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <span style={{ width: `${Math.min(100, Math.max(0, course.attendancePercentage))}%` }} />
              </div>
            </Card>
          ))}
        </div>
      </section>

      {alerts.length > 0 && (
        <Card className="student-alerts" role="status">
          <h2><AlertTriangle size={18} aria-hidden="true" /> Academic alerts</h2>
          <ul>{alerts.map((alert, index) => <li key={`${alert}-${index}`}>{alert}</li>)}</ul>
        </Card>
      )}
    </>
  )
}
