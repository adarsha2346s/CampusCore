import { ArrowRight, BookOpen, Building2, GraduationCap, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'

const workspaces = [
  { title: 'Student directory', description: 'Read student profile information available for academic work.', to: '/faculty/students', icon: GraduationCap },
  { title: 'Course catalog', description: 'Browse the current campus course catalog.', to: '/faculty/courses', icon: BookOpen },
  { title: 'Departments', description: 'View department names and codes.', to: '/faculty/departments', icon: Building2 },
]

export function FacultyOverviewPage() {
  return (
    <div className="role-workspace-page">
      <PageHeader eyebrow="Faculty workspace" title="Overview" description="Your academic workspace, built around the information available to your account." />
      <Card className="role-notice" role="status">
        <span className="role-notice__icon"><ShieldCheck size={20} aria-hidden="true" /></span>
        <div>
          <h2>Teaching assignments are not available here yet</h2>
          <p>The current API does not expose a faculty dashboard or course assignment lookup. The course catalog below is a campus catalog, not a list of courses assigned to you.</p>
          <p>Attendance, assessment and marks endpoints currently require an ADMIN role, so those workflows are not shown in this workspace.</p>
        </div>
      </Card>
      <section className="role-workspace-grid" aria-label="Available faculty workspaces">
        {workspaces.map(({ title, description, to, icon: Icon }) => (
          <Link className="role-workspace-link" to={to} key={to}>
            <Card className="role-workspace-link__card">
              <span className="role-workspace-link__icon"><Icon size={20} aria-hidden="true" /></span>
              <span className="role-workspace-link__copy"><strong>{title}</strong><small>{description}</small></span>
              <ArrowRight size={17} aria-hidden="true" />
            </Card>
          </Link>
        ))}
      </section>
    </div>
  )
}
