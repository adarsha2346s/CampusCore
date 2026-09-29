import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, BookOpen, GraduationCap } from 'lucide-react'
import { ApiError } from '../../lib/api/api-error'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { PageHeader } from '../../components/ui/PageHeader'
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

  return (
    <div className="role-workspace-page">
      <PageHeader eyebrow="Student portal" title="Overview" description="Your personal academic information from the campus records." />
      {profile.isPending ? <LoadingState label="Loading your student profile" />
        : profile.isError ? profile.error instanceof ApiError && profile.error.status === 404
          ? <StudentIdentityNotice section="dashboard" />
          : <ErrorState error={profile.error} onRetry={() => void profile.refetch()} />
          : <>
            <Card className="student-welcome-card">
              <span className="student-welcome-card__icon"><GraduationCap size={21} aria-hidden="true" /></span>
              <div><p className="eyebrow">Student record</p><h2>{fullName(profile.data.firstName, profile.data.lastName)}</h2><p>{profile.data.enrollmentNumber} · {profile.data.departmentName}</p></div>
              <Badge>{profile.data.status}</Badge>
            </Card>
            {dashboard.isPending ? <LoadingState label="Loading your academic summary" />
              : dashboard.isError ? <ErrorState error={dashboard.error} onRetry={() => void dashboard.refetch()} />
                : dashboard.data.courses.length === 0 ? <EmptyState title="No course enrollments yet" description="Your student profile is linked, but the academic system has no course enrollments to include in your dashboard." />
                  : <>
                    <section className="student-summary-grid" aria-label="Academic summary">
                      <Card className="metric-card"><div className="metric-card__icon"><GraduationCap size={19} aria-hidden="true" /></div><p className="metric-card__label">Cumulative GPA</p><p className="metric-card__value">{dashboard.data.gpa.toFixed(2)}</p><p className="metric-card__detail">Calculated by CampusCore</p></Card>
                      <Card className="metric-card"><div className="metric-card__icon metric-card__icon--teal"><BookOpen size={19} aria-hidden="true" /></div><p className="metric-card__label">Enrolled courses</p><p className="metric-card__value">{dashboard.data.courses.length}</p><p className="metric-card__detail">Current student dashboard response</p></Card>
                    </section>
                    <section className="student-dashboard-section" aria-labelledby="student-courses-title">
                      <div className="role-workspace-section-heading"><p className="eyebrow">Academic record</p><h2 id="student-courses-title">My courses</h2></div>
                      <div className="student-course-grid">{dashboard.data.courses.map((course) => <Card className="student-course-card" key={course.courseId}>
                        <div className="student-course-card__heading"><span className="entity-icon entity-icon--blue"><BookOpen size={17} aria-hidden="true" /></span><Badge>{course.credits} credits</Badge></div>
                        <p className="eyebrow">{course.courseCode}</p><h3>{course.courseName}</h3>
                        <div className="student-course-card__attendance"><span>Attendance</span><strong>{course.attendancePercentage.toFixed(1)}%</strong></div>
                      </Card>)}</div>
                    </section>
                    {dashboard.data.alerts.length > 0 && <Card className="student-alerts" role="status"><h2><AlertTriangle size={18} aria-hidden="true" /> Academic alerts</h2><ul>{dashboard.data.alerts.map((alert, index) => <li key={`${alert}-${index}`}>{alert}</li>)}</ul></Card>}
                  </>}
          </>}
    </div>
  )
}
