import { useQuery } from '@tanstack/react-query'
import { ArrowRight, BookOpen, Building2, GraduationCap, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { ApiError } from '../../lib/api/api-error'
import { facultyKeys, getMyFacultyProfile } from '../faculty/faculty.api'

const workspaces = [
  { title: 'Student directory', description: 'Read student profile information available for academic work.', to: '/faculty/students', icon: GraduationCap },
  { title: 'Course catalog', description: 'Browse the current campus course catalog.', to: '/faculty/courses', icon: BookOpen },
  { title: 'Departments', description: 'View department names and codes.', to: '/faculty/departments', icon: Building2 },
]

export function FacultyOverviewPage() {
  const profile = useQuery({ queryKey: facultyKeys.self, queryFn: getMyFacultyProfile })
  return (
    <div className="role-workspace-page">
      <PageHeader eyebrow="Faculty workspace" title="Overview" description="Your faculty identity and the academic directories available to your role." />
      {profile.isPending ? <LoadingState label="Loading your faculty profile" />
        : profile.isError ? profile.error instanceof ApiError && profile.error.status === 404
          ? <Card className="role-notice" role="status"><span className="role-notice__icon"><ShieldCheck size={20} aria-hidden="true" /></span><div><p className="eyebrow role-notice__eyebrow">Account setup</p><h2>Faculty profile link required</h2><p>This account is authenticated as faculty, but no faculty profile is linked to it. Ask a campus administrator to verify the account-to-profile association.</p></div></Card>
          : <ErrorState error={profile.error} onRetry={() => void profile.refetch()} />
          : <>
            <Card className="faculty-profile-card">
              <span className="role-notice__icon"><GraduationCap size={20} aria-hidden="true" /></span>
              <div className="faculty-profile-card__copy"><p className="eyebrow">Faculty identity</p><h2>{[profile.data.firstName, profile.data.lastName].filter(Boolean).join(' ')}</h2><p>{profile.data.employeeNumber} · {profile.data.departmentName}</p></div>
              <Badge>{profile.data.status}</Badge>
            </Card>
            <Card className="role-notice" role="status">
              <span className="role-notice__icon"><ShieldCheck size={20} aria-hidden="true" /></span>
              <div>
                <p className="eyebrow role-notice__eyebrow">Access scope</p>
                <h2>Academic directories for your faculty account</h2>
                <p>Browse the student directory, campus course catalog, and department directory from the links below. Course listings are campus-wide and do not represent teaching assignments.</p>
                <p>The current data model has no faculty-course assignment relationship. Attendance, assessment, and marks workflows remain restricted to administrators.</p>
              </div>
            </Card>
            <section aria-labelledby="faculty-workspace-links-title">
              <div className="role-workspace-section-heading"><p className="eyebrow">Available in this workspace</p><h2 id="faculty-workspace-links-title">Academic directories</h2></div>
              <div className="role-workspace-grid" aria-label="Available faculty workspaces">
                {workspaces.map(({ title, description, to, icon: Icon }) => (
                  <Link className="role-workspace-link" to={to} key={to}>
                    <Card className="role-workspace-link__card"><span className="role-workspace-link__icon"><Icon size={20} aria-hidden="true" /></span><span className="role-workspace-link__copy"><strong>{title}</strong><small>{description}</small></span><ArrowRight size={17} aria-hidden="true" /></Card>
                  </Link>
                ))}
              </div>
            </section>
          </>}
      {profile.isSuccess && profile.data.status === 'INACTIVE' && <EmptyState title="Faculty profile inactive" description="Your faculty profile is currently marked inactive. Contact a campus administrator if you believe this is incorrect." />}
    </div>
  )
}
