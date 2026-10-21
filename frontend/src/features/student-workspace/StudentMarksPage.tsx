import { useQuery } from '@tanstack/react-query'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { getMyMarks, studentMarkKeys } from '../marks/marks.api'
import type { StudentMark } from '../marks/marks.api'

/**
 * Percentage of the assessment maximum, or null when either side is unusable.
 * Shown as the headline figure because that is how a grade is normally read; the
 * raw awarded value stays visible beside it.
 */
function percentageOf(mark: StudentMark): number | null {
  const max = Number(mark.maxMarks)
  const obtained = Number(mark.marksObtained)
  if (!Number.isFinite(max) || max <= 0 || !Number.isFinite(obtained)) return null
  return Math.round((obtained / max) * 1000) / 10
}

/**
 * The signed-in student's own marks.
 *
 * One request, resolved entirely on the server: GET /api/v1/marks/me takes no
 * identifier and returns this student's marks with the course and assessment
 * already joined in. The previous implementation called the marks of every
 * enrollment separately and then looked course and assessment names up from other
 * listings, which cost one request per enrollment and silently degraded to a bare
 * "Course #12" whenever a lookup listing was paged past the relevant row.
 *
 * Ownership is therefore never decided in the browser, and there is no identifier
 * that could be swapped for someone else's.
 */
export function StudentMarksPage() {
  const marks = useQuery({
    queryKey: studentMarkKeys.mine,
    queryFn: getMyMarks,
  })

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="Academic"
        title="My marks"
        description="Marks recorded against your own enrollments. The server scopes every record below to your account."
      />

      {marks.isPending && <LoadingState label="Loading your marks" />}

      {marks.isError && (
        <ErrorState error={marks.error} onRetry={() => void marks.refetch()} />
      )}

      {marks.data?.length === 0 && (
        <EmptyState
          title="No marks recorded yet"
          description="Marks appear here once faculty enter them against your assessments. If you are expecting results, check that your enrollment is recorded for the term."
        />
      )}

      {marks.data && marks.data.length > 0 && (
        <Card>
          <ResourceTable
            caption="Marks recorded for your enrollments"
            columns={[
              {
                key: 'course',
                header: 'Course',
                render: (mark: StudentMark) => (
                  <div className="identity-cell">
                    <span className="identity-avatar" aria-hidden="true">
                      {mark.courseCode.slice(0, 1)}
                    </span>
                    <span>
                      <strong>{mark.courseName}</strong>
                      <small>{mark.courseCode}</small>
                    </span>
                  </div>
                ),
              },
              {
                key: 'term',
                header: 'Term',
                mobileHidden: true,
                render: (mark: StudentMark) => `${mark.semester} ${mark.academicYear}`,
              },
              {
                key: 'assessment',
                header: 'Assessment',
                render: (mark: StudentMark) => (
                  <span>
                    <strong>{mark.assessmentName}</strong>
                    <small className="cell-subtext">
                      {mark.assessmentType} · {mark.weight}% weight
                    </small>
                  </span>
                ),
              },
              {
                key: 'mark',
                header: 'Mark',
                render: (mark: StudentMark) => {
                  const percentage = percentageOf(mark)
                  return (
                    <span>
                      <strong className="mark-value">
                        {mark.marksObtained}
                        <small> / {mark.maxMarks}</small>
                      </strong>
                      {percentage !== null && (
                        <small className="cell-subtext">{percentage}%</small>
                      )}
                    </span>
                  )
                },
              },
              {
                key: 'recorded',
                header: 'Recorded',
                mobileHidden: true,
                render: (mark: StudentMark) =>
                  mark.enteredAt ? (
                    <span className="muted">
                      {new Date(mark.enteredAt).toLocaleDateString()}
                    </span>
                  ) : (
                    <span className="muted">—</span>
                  ),
              },
            ]}
            rows={marks.data}
            getRowKey={(mark) => mark.markId}
          />
        </Card>
      )}
    </div>
  )
}