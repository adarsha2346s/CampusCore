import { useQuery } from '@tanstack/react-query'
import { ArrowRight, BookOpen, Building2, GraduationCap, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Card } from '../../components/ui/Card'
import { Badge } from '../../components/ui/Badge'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { PageHeader } from '../../components/ui/PageHeader'
import { AnimatedNumber } from '../../components/data-display/AnimatedNumber'
import { BarList } from '../../components/charts/BarList'
import { MetricStrip } from '../../components/charts/MetricStrip'
import { ApiError } from '../../lib/api/api-error'
import { getCoursesPage, courseKeys } from '../courses/courses.api'
import { getDepartments, departmentKeys } from '../departments/departments.api'
import { getStudents, studentKeys } from '../students/students.api'
import { facultyKeys, getMyFacultyProfile } from '../faculty/faculty.api'
import { studentsByDepartment } from '../../lib/format/analytics'

const workspaces = [
  { title: 'Student directory', description: 'Read student profile information available for academic work.', to: '/faculty/students', icon: GraduationCap },
  { title: 'Course catalog', description: 'Browse the current campus course catalog.', to: '/faculty/courses', icon: BookOpen },
  { title: 'Departments', description: 'View department names and codes.', to: '/faculty/departments', icon: Building2 },
]

export function FacultyOverviewPage() {
  const profile = useQuery({ queryKey: facultyKeys.self, queryFn: getMyFacultyProfile })
  const students = useQuery({ queryKey: studentKeys.all, queryFn: getStudents })
  const courses = useQuery({ queryKey: courseKeys.page(0, 1), queryFn: () => getCoursesPage(0, 1) })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })

  const distribution = studentsByDepartment(students.data ?? [], departments.data ?? [])
    .filter((entry) => entry.students > 0)

  return (
    <div className="role-workspace-page">
      <PageHeader eyebrow="Faculty workspace" title="Overview" description="Your faculty identity and the academic directories available to your role." />
      {profile.isPending ? <LoadingState label="Loading your faculty profile" />
        : profile.isError ? profile.error instanceof ApiError && profile.error.status === 404
          ? <Card className="role-notice" role="status">
            <span className="role-notice__icon"><ShieldCheck size={20} aria-hidden="true" /></span>
            <div>
              <p className="eyebrow role-notice__eyebrow">Account setup</p>
              <h2>Faculty profile link required</h2>
              <p>This account is authenticated as faculty, but no faculty profile is linked to it. Ask a campus administrator to verify the account-to-profile association.</p>
            </div>
          </Card>
          : <ErrorState error={profile.error} onRetry={() => void profile.refetch()} />
          : <>
            <Card className="identity-card">
              <span className="identity-card__icon"><GraduationCap size={21} aria-hidden="true" /></span>
              <div>
                <p className="eyebrow">Faculty identity</p>
                <h2>{[profile.data.firstName, profile.data.lastName].filter(Boolean).join(' ')}</h2>
                <p>{profile.data.employeeNumber} · {profile.data.departmentName}</p>
              </div>
              <Badge>{profile.data.status}</Badge>
            </Card>

            <MetricStrip
              label="Campus directory summary"
              items={[
                { label: 'Student profiles', value: <AnimatedNumber value={students.data?.length ?? 0} />, detail: 'Campus wide' },
                { label: 'Course offerings', value: <AnimatedNumber value={courses.data?.totalElements ?? 0} />, detail: 'Campus wide' },
                { label: 'Departments', value: <AnimatedNumber value={departments.data?.length ?? 0} />, detail: 'Academic units' },
                { label: 'Directory status', value: students.isError || courses.isError || departments.isError ? 'Offline' : 'Live', detail: 'Read only' },
              ]}
            />

            <Card className="role-notice" role="status">
              <span className="role-notice__icon"><ShieldCheck size={20} aria-hidden="true" /></span>
              <div>
                <p className="eyebrow role-notice__eyebrow">Access scope</p>
                <h2>Academic directories for your faculty account</h2>
                <p>Browse the student directory, campus course catalog, and department directory from the links below. Course listings are campus-wide and do not represent teaching assignments.</p>
                <p>The current data model has no faculty-course assignment relationship. Attendance, assessment, and marks workflows remain restricted to administrators.</p>
              </div>
            </Card>

            {distribution.length > 0 && (
              <Card>
                <div className="section-heading">
                  <div>
                    <p className="eyebrow">Directory shape</p>
                    <h2>Departments in this workspace</h2>
                  </div>
                </div>
                <BarList
                  items={distribution.map((entry) => ({ label: entry.name, meta: entry.code, value: entry.students }))}
                  label="Departments available in the faculty workspace"
                  caption="Faculty accounts see the campus directory; teaching assignments are not modelled."
                />
              </Card>
            )}

            <section aria-labelledby="faculty-workspace-links-title">
              <div className="role-workspace-section-heading">
                <p className="eyebrow">Available in this workspace</p>
                <h2 id="faculty-workspace-links-title">Academic directories</h2>
              </div>
              <div className="role-workspace-grid" aria-label="Available faculty workspaces">
                {workspaces.map(({ title, description, to, icon: Icon }) => (
                  <Link className="role-workspace-link" to={to} key={to} viewTransition>
                    <Card className="role-workspace-link__card">
                      <span className="quick-link__icon"><Icon size={19} aria-hidden="true" /></span>
                      <span className="role-workspace-link__copy"><strong>{title}</strong><small>{description}</small></span>
                      <ArrowRight size={17} aria-hidden="true" />
                    </Card>
                  </Link>
                ))}
              </div>
            </section>
          </>}
      {profile.isSuccess && profile.data.status === 'INACTIVE' && (
        <Card className="role-notice" role="status">
          <span className="role-notice__icon"><ShieldCheck size={20} aria-hidden="true" /></span>
          <div>
            <h2>Faculty profile inactive</h2>
            <p>Your faculty profile is currently marked inactive. Contact a campus administrator if you believe this is incorrect.</p>
          </div>
        </Card>
      )}
    </div>
  )
}