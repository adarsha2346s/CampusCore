import { useQuery } from '@tanstack/react-query'
import { Card } from '../../components/ui/Card'
import { Badge } from '../../components/ui/Badge'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { PageHeader } from '../../components/ui/PageHeader'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { enrollmentKeys, getMyEnrollments } from '../enrollments/enrollments.api'
import { courseKeys, getCourses } from '../courses/courses.api'
import type { EnrollmentResponse } from '../../types/api'

/**
 * The signed-in student's own enrollments.
 *
 * Backed by GET /api/v1/enrollments/me, which is student-only and returns just
 * the caller's records, so no identifier is taken from the URL and no ownership
 * decision is made in the browser.
 */
export function StudentEnrollmentsPage() {
  const enrollments = useQuery({
    queryKey: enrollmentKeys.mine,
    queryFn: getMyEnrollments,
  })

  // The full authenticated catalog rather than a fixed first page. Asking for
  // page 0 of 100 meant any enrollment past the hundredth course silently rendered
  // as "Course #412" with no code, which reads as missing data rather than as a
  // paging limit.
  const courses = useQuery({
    queryKey: courseKeys.all,
    queryFn: getCourses,
  })

  const courseById = new Map(
    (courses.data ?? []).map((course) => [course.courseId, course]),
  )

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="Academic"
        title="My courses"
        description="Courses you are enrolled in this academic year, exactly as the records hold them."
      />

      {enrollments.isPending && <LoadingState label="Loading your enrollments" />}

      {enrollments.isError && (
        <ErrorState error={enrollments.error} onRetry={() => void enrollments.refetch()} />
      )}

      {enrollments.data && enrollments.data.length === 0 && (
        <EmptyState
          title="No enrollments yet"
          description="You are not enrolled in any course. Enrollments are recorded by the registrar."
        />
      )}

      {enrollments.data && enrollments.data.length > 0 && (
        <Card>
          <ResourceTable
            caption="Your course enrollments"
            columns={[
              {
                key: 'course',
                header: 'Course',
                render: (row: EnrollmentResponse) => {
                  const course = courseById.get(row.courseId)
                  return (
                    <div className="identity-cell">
                      <span className="identity-avatar" aria-hidden="true">
                        {(course?.courseCode ?? '?').slice(0, 1)}
                      </span>
                      <span>
                        <strong>{course?.courseName ?? `Course ${row.courseId}`}</strong>
                        <small>{course?.courseCode ?? 'Course details unavailable'}</small>
                      </span>
                    </div>
                  )
                },
              },
              {
                key: 'term',
                header: 'Term',
                render: (row: EnrollmentResponse) => `${row.semester} ${row.academicYear}`,
              },
              {
                key: 'status',
                header: 'Status',
                render: (row: EnrollmentResponse) => <Badge>{row.status}</Badge>,
              },
            ]}
            rows={enrollments.data}
            getRowKey={(row) => row.enrollmentId}
          />
        </Card>
      )}
    </div>
  )
}
