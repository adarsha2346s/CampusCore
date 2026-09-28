import { useQuery } from '@tanstack/react-query'
import { ArrowRight, BookOpen, Building2, GraduationCap, Users, UserRound, Layers3 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ErrorState } from '../../components/data-display/ErrorState'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { getUsers, userKeys } from '../users/users.api'
import { getStudents, studentKeys } from '../students/students.api'
import { getFaculty, facultyKeys } from '../faculty/faculty.api'
import { getDepartments, departmentKeys } from '../departments/departments.api'
import { getCourses, courseKeys } from '../courses/courses.api'

const quickLinks = [
  { label: 'Manage students', to: '/admin/students', description: 'Review student profiles', icon: GraduationCap },
  { label: 'Manage faculty', to: '/admin/faculty', description: 'Browse faculty records', icon: UserRound },
  { label: 'Course catalog', to: '/admin/courses', description: 'Maintain academic offerings', icon: BookOpen },
  { label: 'User accounts', to: '/admin/users', description: 'Manage campus access', icon: Users },
]

export function AdminDashboardPage() {
  const usersQuery = useQuery({ queryKey: userKeys.all, queryFn: getUsers })
  const studentsQuery = useQuery({ queryKey: studentKeys.all, queryFn: getStudents })
  const facultyQuery = useQuery({ queryKey: facultyKeys.all, queryFn: getFaculty })
  const departmentsQuery = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const coursesQuery = useQuery({ queryKey: courseKeys.all, queryFn: getCourses })
  const overviewQueries = [usersQuery, studentsQuery, facultyQuery, departmentsQuery, coursesQuery]

  if (overviewQueries.some((query) => query.isPending)) return (
    <div className="admin-page">
      <PageHeader eyebrow="Campus operations" title="Overview" description="A current view of the records available in CampusCore." />
      <div className="metric-grid" aria-hidden="true">{Array.from({ length: 5 }, (_, index) => <div className="card dashboard-skeleton" key={index}><span /><span /><span /><span /></div>)}</div>
      <div className="overview-lower-grid" aria-hidden="true"><div className="card dashboard-skeleton dashboard-skeleton--large" /><div className="card dashboard-skeleton dashboard-skeleton--large" /></div>
      <span className="sr-only" role="status">Loading campus overview</span>
    </div>
  )
  const failedQuery = overviewQueries.find((query) => query.isError)
  if (failedQuery) return <><PageHeader eyebrow="Campus operations" title="Overview" description="A current view of the records available in CampusCore." /><ErrorState error={failedQuery.error} onRetry={() => { for (const query of overviewQueries) void query.refetch() }} /></>

  // Query state above guarantees each successful query has resolved. The
  // fallback only satisfies TypeScript; it does not replace failed data.
  const users = usersQuery.data ?? []
  const students = studentsQuery.data ?? []
  const faculty = facultyQuery.data ?? []
  const departments = departmentsQuery.data ?? []
  const courses = coursesQuery.data ?? []
  const hasRecords = users.length + students.length + faculty.length + departments.length + courses.length > 0
  const metrics = [
    { label: 'Total users', value: users.length, detail: `${users.filter((user) => user.active).length} active accounts`, icon: Users, tone: 'blue' },
    { label: 'Students', value: students.length, detail: `${students.filter((student) => student.status === 'ACTIVE').length} active profiles`, icon: GraduationCap, tone: 'teal' },
    { label: 'Faculty', value: faculty.length, detail: `${faculty.filter((member) => member.status === 'ACTIVE').length} active profiles`, icon: UserRound, tone: 'violet' },
    { label: 'Departments', value: departments.length, detail: 'Academic units', icon: Building2, tone: 'amber' },
    { label: 'Courses', value: courses.length, detail: `${courses.filter((course) => course.status === 'ACTIVE').length} active offerings`, icon: BookOpen, tone: 'green' },
  ] as const

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Campus operations" title="Overview" description="A current view of the records available in CampusCore." />
      <section className="metric-grid" aria-label="Campus record totals">
        {metrics.map(({ label, value, detail, icon: Icon, tone }) => (
          <Card key={label} className="metric-card">
            <div className={`metric-card__icon metric-card__icon--${tone}`}><Icon size={19} aria-hidden="true" /></div>
            <p className="metric-card__label">{label}</p>
            <p className="metric-card__value">{value.toLocaleString()}</p>
            <p className="metric-card__detail"><span className="status-dot" aria-hidden="true" />{detail}</p>
          </Card>
        ))}
      </section>

      {!hasRecords && <div className="setup-callout" role="status"><div><strong>Your campus directory is ready to set up.</strong><p>Start with a department, then add courses and link student and faculty profiles to user accounts.</p></div><Button variant="secondary" asChild><Link to="/admin/departments">Add a department <ArrowRight size={15} aria-hidden="true" /></Link></Button></div>}

      <section className="overview-lower-grid" aria-label="Operational summary and quick links">
        <Card className="overview-summary">
          <div className="section-heading">
            <div><p className="eyebrow">Directory health</p><h2>Current records</h2></div>
            <span className="overview-summary__mark"><Layers3 size={19} aria-hidden="true" /></span>
          </div>
          <div className="summary-list">
            <SummaryRow label="Active user accounts" value={users.filter((user) => user.active).length} total={users.length} />
            <SummaryRow label="Active student profiles" value={students.filter((student) => student.status === 'ACTIVE').length} total={students.length} />
            <SummaryRow label="Active faculty profiles" value={faculty.filter((member) => member.status === 'ACTIVE').length} total={faculty.length} />
            <SummaryRow label="Active courses" value={courses.filter((course) => course.status === 'ACTIVE').length} total={courses.length} />
          </div>
          <p className="muted overview-summary__note">Counts are composed from the existing admin list APIs. The backend does not provide a dedicated admin dashboard feed.</p>
        </Card>

        <Card className="quick-links-card">
          <div className="section-heading"><div><p className="eyebrow">Shortcuts</p><h2>Common tasks</h2></div></div>
          <div className="quick-links">
            {quickLinks.map(({ label, to, description, icon: Icon }) => (
              <Link className="quick-link" to={to} key={to}>
                <span className="quick-link__icon"><Icon size={18} aria-hidden="true" /></span>
                <span className="quick-link__text"><strong>{label}</strong><small>{description}</small></span>
                <ArrowRight size={16} aria-hidden="true" />
              </Link>
            ))}
          </div>
          <Button variant="secondary" asChild><Link to="/admin/departments">Browse departments <ArrowRight size={15} aria-hidden="true" /></Link></Button>
        </Card>
      </section>
    </div>
  )
}

function SummaryRow({ label, value, total }: { label: string; value: number; total: number }) {
  const ratio = total > 0 ? Math.min(100, Math.round((value / total) * 100)) : 0
  return (
    <div className="summary-row">
      <div className="summary-row__top"><span>{label}</span><strong>{value}<small> / {total}</small></strong></div>
      <div className="summary-row__track" role="progressbar" aria-label={label} aria-valuenow={ratio} aria-valuemin={0} aria-valuemax={100}><span style={{ width: `${ratio}%` }} /></div>
    </div>
  )
}
