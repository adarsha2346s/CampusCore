import { useQuery } from '@tanstack/react-query'
import { Card } from '../../components/ui/Card'
import { Badge } from '../../components/ui/Badge'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { PageHeader } from '../../components/ui/PageHeader'
import { facultyCourseKeys, getFacultyCourses, getMyFacultyProfile } from '../faculty/faculty.api'
import { formatDate } from '../../lib/format/datetime'

/**
 * Courses currently assigned to the signed-in faculty member.
 *
 * Backed by GET /api/v1/faculty/{id}/courses, which the API answers only for an
 * administrator or that faculty member, and which reflects the assignment
 * relationship rather than the whole campus catalog. When nothing is assigned
 * the page says so rather than showing invented rows.
 */
export function FacultyCoursesPage() {
  const profile = useQuery({
    queryKey: ['faculty', 'self'],
    queryFn: getMyFacultyProfile,
  })

  const facultyId = profile.data?.facultyId
  const hasId = typeof facultyId === 'number'

  const courses = useQuery({
    queryKey: facultyCourseKeys.mine(facultyId ?? 0),
    queryFn: () => getFacultyCourses(facultyId as number),
    enabled: hasId,
  })

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="Teaching"
        title="My courses"
        description="Courses you are currently assigned to teach. Access to assessments, marks and attendance for a course follows this assignment."
      />

      {profile.isPending && <LoadingState label="Loading your faculty profile" />}

      {profile.isError && (
        <ErrorState error={profile.error} onRetry={() => void profile.refetch()} />
      )}

      {profile.data && (
        <Card as="section">
          <dl className="metric-strip">
            <div className="metric-strip__item">
              <dt className="metric-strip__label">Faculty</dt>
              <dd className="metric-strip__value">
                {profile.data.firstName} {profile.data.lastName ?? ''}
              </dd>
            </div>
            <div className="metric-strip__item">
              <dt className="metric-strip__label">Employee number</dt>
              <dd className="metric-strip__value">{profile.data.employeeNumber}</dd>
            </div>
            <div className="metric-strip__item">
              <dt className="metric-strip__label">Department</dt>
              <dd className="metric-strip__value">{profile.data.departmentName}</dd>
            </div>
            <div className="metric-strip__item">
              <dt className="metric-strip__label">Status</dt>
              <dd className="metric-strip__value">
                <Badge>{profile.data.status}</Badge>
              </dd>
            </div>
          </dl>
        </Card>
      )}

      {hasId && courses.isPending && <LoadingState label="Loading assigned courses" />}

      {courses.isError && (
        <ErrorState error={courses.error} onRetry={() => void courses.refetch()} />
      )}

      {courses.data && courses.data.length === 0 && (
        <EmptyState
          title="No courses assigned yet"
          description="An administrator assigns faculty to courses. Once you are assigned, the course and its academic records become reachable from this workspace."
        />
      )}

      {courses.data && courses.data.length > 0 && (
        <ul className="student-course-grid">
          {courses.data.map((assignment) => (
            <li key={assignment.assignmentId}>
              <Card as="article" className="student-course-card">
                <span className="metric-strip__label">{assignment.courseCode}</span>
                <h2 className="page-header__title">{assignment.courseName}</h2>
                <p className="student-course-card__meta">
                  Assigned {formatDate(assignment.assignedAt) ?? 'recently'}
                </p>
                <Badge>{assignment.active ? 'Current' : 'Ended'}</Badge>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
