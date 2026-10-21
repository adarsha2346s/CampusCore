import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { PageHeader } from '../../components/ui/PageHeader'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { MyCoursePicker } from './MyCoursePicker'
import { facultyTeachingKeys, useMyCourses } from './useMyCourses'
import { getAssessmentsByCourse } from '../assessments/assessments.api'
import type { AssessmentResponse } from '../../types/api'

/**
 * Assessments for a course the signed-in faculty member teaches.
 *
 * Reads GET /api/v1/assessments/course/{courseId}. The institution-wide
 * assessment listing is administrator-only, so this page is deliberately driven
 * from a course assignment instead of a global fetch. The server rejects the
 * request outright if the course is not assigned, which is the real control; the
 * picker merely avoids asking.
 */
export function FacultyAssessmentsPage() {
  const { courses, isPending, isError, error, refetch } = useMyCourses()
  const [courseId, setCourseId] = useState<number | null>(null)

  const activeCourseId = courseId ?? courses[0]?.courseId ?? null

  const assessments = useQuery({
    queryKey: facultyTeachingKeys.assessments(activeCourseId ?? 0),
    queryFn: () => getAssessmentsByCourse(activeCourseId as number),
    enabled: activeCourseId !== null,
  })

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="Teaching"
        title="Assessments"
        description="Assessments defined for the courses you are assigned to teach."
      />

      <Card as="section">
        <MyCoursePicker
          selectedCourseId={activeCourseId}
          onSelect={setCourseId}
          scopingHint="Assessments are scoped to the selected assigned course."
        />
      </Card>

      {isPending && <p className="faculty-scope__hint">Loading your courses…</p>}

      {isError && <ErrorState error={error} onRetry={refetch} />}

      {assessments.isPending && activeCourseId !== null && (
        <p className="faculty-scope__hint" role="status">
          Loading assessments…
        </p>
      )}

      {assessments.isError && (
        <ErrorState error={assessments.error} onRetry={() => void assessments.refetch()} />
      )}

      {assessments.data && assessments.data.length === 0 && (
        <EmptyState
          title="No assessments yet"
          description="This course has no assessments defined. An administrator adds them."
        />
      )}

      {assessments.data && assessments.data.length > 0 && (
        <Card>
          <ResourceTable
            caption="Assessments for the selected course"
            columns={[
              {
                key: 'name',
                header: 'Assessment',
                render: (row: AssessmentResponse) => (
                  <div className="identity-cell">
                    <span className="identity-avatar" aria-hidden="true">
                      {row.name.slice(0, 1)}
                    </span>
                    <span>
                      <strong>{row.name}</strong>
                      <small>{row.assessmentType.replace('_', ' ').toLowerCase()}</small>
                    </span>
                  </div>
                ),
              },
              {
                key: 'max',
                header: 'Max marks',
                render: (row: AssessmentResponse) => row.maxMarks,
              },
              {
                key: 'weight',
                header: 'Weight',
                mobileHidden: true,
                render: (row: AssessmentResponse) => `${row.weight}%`,
              },
              {
                key: 'date',
                header: 'Date',
                mobileHidden: true,
                render: (row: AssessmentResponse) =>
                  row.assessmentDate
                    ? new Date(row.assessmentDate).toLocaleDateString()
                    : 'Not scheduled',
              },
            ]}
            rows={assessments.data}
            getRowKey={(row) => row.assessmentId}
          />
        </Card>
      )}
    </div>
  )
}
