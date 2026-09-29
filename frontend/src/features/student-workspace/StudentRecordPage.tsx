import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { SelectField } from '../../components/forms/SelectField'
import { PageHeader } from '../../components/ui/PageHeader'
import { ApiError } from '../../lib/api/api-error'
import { enrollmentKeys, getMyEnrollments } from '../enrollments/enrollments.api'
import { getEnrollmentGpa, gpaKeys } from '../grading/grading.api'
import { getMyStudentProfile, studentKeys } from '../students/students.api'
import { StudentIdentityNotice } from './StudentIdentityNotice'

function fullName(firstName: string, lastName: string | null) {
  return [firstName, lastName].filter(Boolean).join(' ')
}

export function StudentRecordPage({ section }: { section: 'profile' | 'gpa' }) {
  const [enrollmentId, setEnrollmentId] = useState('')
  const title = section === 'profile' ? 'My profile' : 'GPA'
  const description = section === 'profile' ? 'Your student identity and profile details.' : 'Course GPA results calculated by the academic records service.'
  const profile = useQuery({ queryKey: studentKeys.self, queryFn: getMyStudentProfile })
  const enrollments = useQuery({ queryKey: enrollmentKeys.mine, queryFn: getMyEnrollments, enabled: section === 'gpa' && profile.isSuccess })
  const gpa = useQuery({
    queryKey: gpaKeys.enrollment(enrollmentId, ''),
    queryFn: () => getEnrollmentGpa(Number(enrollmentId)),
    enabled: section === 'gpa' && Boolean(enrollmentId) && profile.isSuccess,
  })

  return (
    <div className="role-workspace-page">
      <PageHeader eyebrow="Student portal" title={title} description={description} />
      {profile.isPending ? <LoadingState label="Loading your student profile" />
        : profile.isError ? profile.error instanceof ApiError && profile.error.status === 404
          ? <StudentIdentityNotice section={section} />
          : <ErrorState error={profile.error} onRetry={() => void profile.refetch()} />
          : section === 'profile' ? <Card className="student-profile-card">
            <div className="student-profile-card__heading"><div><p className="eyebrow">Personal academic record</p><h2>{fullName(profile.data.firstName, profile.data.lastName)}</h2></div><Badge>{profile.data.status}</Badge></div>
            <dl className="student-profile-grid">
              <div><dt>Enrollment number</dt><dd>{profile.data.enrollmentNumber}</dd></div>
              <div><dt>Department</dt><dd>{profile.data.departmentName}</dd></div>
              <div><dt>Admission year</dt><dd>{profile.data.admissionYear}</dd></div>
            </dl>
          </Card> : <>
            <Card className="student-gpa-lookup">
              <p className="eyebrow">Your enrollments</p>
              <h2>Choose a course enrollment</h2>
              <p className="muted">GPA is returned for one enrollment at a time. Select the course record you want to review.</p>
              {enrollments.isPending ? <LoadingState label="Loading your enrollments" />
                : enrollments.isError ? <ErrorState error={enrollments.error} onRetry={() => void enrollments.refetch()} />
                  : enrollments.data.length === 0 ? <EmptyState title="No enrollments available" description="There are no enrollment records linked to your student profile, so a course GPA cannot be requested." />
                    : <SelectField id="student-enrollment" label="Course enrollment" value={enrollmentId} onChange={(event) => setEnrollmentId(event.target.value)}>
                      <option value="">Select an enrollment</option>
                      {enrollments.data.map((enrollment) => <option key={enrollment.enrollmentId} value={enrollment.enrollmentId}>Course #{enrollment.courseId} · {enrollment.semester} {enrollment.academicYear} · {enrollment.status}</option>)}
                    </SelectField>}
            </Card>
            {enrollmentId && (gpa.isPending ? <LoadingState label="Requesting your GPA result" />
              : gpa.isError ? <ErrorState error={gpa.error} onRetry={() => void gpa.refetch()} />
                : gpa.data && <Card className="student-gpa-result" aria-live="polite">
                  <div className="student-gpa-result__grade"><span>Grade</span><strong>{gpa.data.grade}</strong><small>{gpa.data.gradePoint} grade points</small></div>
                  <div className="student-gpa-result__course"><p className="eyebrow">{gpa.data.courseCode}</p><h2>{gpa.data.courseName}</h2><p>{gpa.data.currentPercentage}% current percentage · {gpa.data.assessedWeight}% assessed weight</p><Badge className={gpa.data.complete ? 'result-complete' : 'result-in-progress'}>{gpa.data.complete ? 'Complete' : 'In progress'}</Badge></div>
                </Card>)}
          </>}
    </div>
  )
}
