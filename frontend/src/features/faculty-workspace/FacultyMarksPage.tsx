import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card } from '../../components/ui/Card'
import { Badge } from '../../components/ui/Badge'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { PageHeader } from '../../components/ui/PageHeader'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { SelectField } from '../../components/forms/SelectField'
import { MyCoursePicker } from './MyCoursePicker'
import { facultyTeachingKeys, useMyCourses } from './useMyCourses'
import { getEnrollmentsByCourse } from '../enrollments/enrollments.api'
import { getMarksByEnrollment } from '../marks/marks.api'
import { getAssessmentsByCourse } from '../assessments/assessments.api'
import type { AssessmentResponse, EnrollmentResponse, MarkResponse } from '../../types/api'

type ScopedMark = MarkResponse & { enrollment: EnrollmentResponse }

/**
 * Marks for a course the signed-in faculty member teaches.
 *
 * Chain of scope-checked reads: pick an assigned course, list that course's
 * enrollments, then read the marks of those enrollments. Every hop is authorised
 * server-side by the course assignment, so no row from an untaught course can be
 * requested. The institution-wide mark listing is administrator-only and is not
 * used here.
 */
export function FacultyMarksPage() {
  const { courses, isPending, isError, error, refetch } = useMyCourses()
  const [courseId, setCourseId] = useState<number | null>(null)
  const [enrollmentId, setEnrollmentId] = useState<number | null>(null)

  const activeCourseId = courseId ?? courses[0]?.courseId ?? null

  const enrollments = useQuery({
    queryKey: facultyTeachingKeys.enrollments(activeCourseId ?? 0),
    queryFn: () => getEnrollmentsByCourse(activeCourseId as number),
    enabled: activeCourseId !== null,
  })

  const assessments = useQuery({
    queryKey: facultyTeachingKeys.assessments(activeCourseId ?? 0),
    queryFn: () => getAssessmentsByCourse(activeCourseId as number),
    enabled: activeCourseId !== null,
  })

  const selectedEnrollmentId = enrollmentId ?? enrollments.data?.[0]?.enrollmentId ?? null

  // Marks are read for the selected enrollment only. The API scopes them that way,
  // and the table below only ever shows the selected student, so reading every
  // enrollment in the course issued one request per student to display one - forty
  // requests on a full course.
  const marks = useQuery({
    queryKey: facultyTeachingKeys.marks(selectedEnrollmentId ?? 0),
    queryFn: () => getMarksByEnrollment(selectedEnrollmentId as number),
    enabled: selectedEnrollmentId !== null,
  })

  const visibleRows: ScopedMark[] = useMemo(() => {
    const enrollment = enrollments.data?.find(
      (item) => item.enrollmentId === selectedEnrollmentId,
    )
    if (!enrollment) return []
    return (marks.data ?? []).map((mark) => ({ ...mark, enrollment }))
  }, [marks.data, enrollments.data, selectedEnrollmentId])

  const studentLabel = (enrollment: EnrollmentResponse) =>
    `${enrollment.studentFirstName} ${enrollment.studentLastName ?? ''}`.trim() ||
    enrollment.enrollmentNumber

  const assessmentById = useMemo(
    () => new Map((assessments.data ?? []).map((a) => [a.assessmentId, a])),
    [assessments.data],
  )

  const marksError = marks.isError ? marks.error : undefined
  const maxByAssessment = (assessmentId: number) =>
    assessmentById.get(assessmentId)?.maxMarks ?? null

  return (
    <div className="admin-page">
      <PageHeader
        eyebrow="Teaching"
        title="Marks"
        description="Marks for students enrolled in the courses you are assigned to teach."
      />

      <Card as="section">
        <MyCoursePicker
          selectedCourseId={activeCourseId}
          onSelect={(next) => {
            setCourseId(next)
            setEnrollmentId(null)
          }}
          scopingHint="Marks are scoped to the selected assigned course."
        />

        {enrollments.isPending && (
          <p className="faculty-scope__hint" role="status">
            Loading enrollments…
          </p>
        )}

        {enrollments.isError && (
          <ErrorState error={enrollments.error} onRetry={() => void enrollments.refetch()} />
        )}

        {enrollments.data && enrollments.data.length > 0 && (
          <SelectField
            id="faculty-enrollment"
            name="faculty-enrollment"
            label="Student enrollment"
            value={String(selectedEnrollmentId)}
            onChange={(event) => setEnrollmentId(Number(event.target.value))}
          >
            {enrollments.data.map((enrollment) => (
              <option key={enrollment.enrollmentId} value={enrollment.enrollmentId}>
                {studentLabel(enrollment)} — {enrollment.semester}{' '}
                {enrollment.academicYear}
              </option>
            ))}
          </SelectField>
        )}
      </Card>

      {isPending && <p className="faculty-scope__hint">Loading your courses…</p>}

      {isError && <ErrorState error={error} onRetry={refetch} />}

      {marksError && <ErrorState error={marksError} onRetry={() => void marks.refetch()} />}

      {enrollments.data && enrollments.data.length === 0 && (
        <EmptyState
          title="No students enrolled"
          description="Nobody is enrolled in the selected course, so there are no marks to show."
        />
      )}

      {enrollments.data && enrollments.data.length > 0 && selectedEnrollmentId !== null && (
        <Card as="section">
          <dl className="metric-strip">
            <div className="metric-strip__item">
              <dt className="metric-strip__label">Student</dt>
              <dd className="metric-strip__value">
                {studentLabel(
                  enrollments.data.find((e) => e.enrollmentId === selectedEnrollmentId)!,
                )}
              </dd>
            </div>
            <div className="metric-strip__item">
              <dt className="metric-strip__label">Enrollment number</dt>
              <dd className="metric-strip__value">
                {enrollments.data.find((e) => e.enrollmentId === selectedEnrollmentId)
                  ?.enrollmentNumber ?? '—'}
              </dd>
            </div>
            <div className="metric-strip__item">
              <dt className="metric-strip__label">Marks recorded</dt>
              <dd className="metric-strip__value">{visibleRows.length}</dd>
            </div>
          </dl>
        </Card>
      )}

      {enrollments.data && enrollments.data.length > 0 && (
        <Card>
          {visibleRows.length === 0 ? (
            <EmptyState
              title="No marks recorded"
              description="Marks appear here once they are entered for this enrollment."
            />
          ) : (
            <ResourceTable
              caption="Marks for the selected student"
              columns={[
                {
                  key: 'assessment',
                  header: 'Assessment',
                  render: (row: ScopedMark) => {
                    const assessment: AssessmentResponse | undefined =
                      assessmentById.get(row.assessmentId)
                    return (
                      <div className="identity-cell">
                        <span className="identity-avatar" aria-hidden="true">
                          {(assessment?.name ?? '?').slice(0, 1)}
                        </span>
                        <span>
                          <strong>{assessment?.name ?? `Assessment #${row.assessmentId}`}</strong>
                          <small>
                            {assessment
                              ? `${assessment.assessmentType.replace('_', ' ').toLowerCase()} · out of ${assessment.maxMarks}`
                              : 'Details unavailable'}
                          </small>
                        </span>
                      </div>
                    )
                  },
                },
                {
                  key: 'mark',
                  header: 'Mark',
                  render: (row: ScopedMark) => <strong>{row.marksObtained}</strong>,
                },
                {
                  key: 'percentage',
                  header: 'Percentage',
                  render: (row: ScopedMark) => {
                    const max = maxByAssessment(row.assessmentId)
                    if (!max || max === 0) return '—'
                    const percentage =
                      (Number(row.marksObtained) / max) * 100
                    return `${percentage.toFixed(1)}%`
                  },
                },
                {
                  key: 'recorded',
                  header: 'Recorded',
                  mobileHidden: true,
                  render: (row: ScopedMark) => (
                    <Badge>{new Date(row.enteredAt).toLocaleDateString()}</Badge>
                  ),
                },
              ]}
              rows={visibleRows}
              getRowKey={(row) => row.markId}
            />
          )}
        </Card>
      )}
    </div>
  )
}
