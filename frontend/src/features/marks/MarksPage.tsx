import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { notifyError } from '../admin-shared/feedback'
import { getAssessments } from '../assessments/assessments.api'
import { getCourses } from '../courses/courses.api'
import { getEnrollments } from '../enrollments/enrollments.api'
import { getStudents } from '../students/students.api'
import { MarkFormDialog } from './MarkFormDialog'
import { createMark, getMark, getMarks, getMarksByAssessment, getMarksByEnrollment, markKeys } from './marks.api'
import type { MarkResponse } from '../../types/api'

export function MarksPage() {
  const client = useQueryClient()
  const [search, setSearch] = useState('')
  const [enrollmentId, setEnrollmentId] = useState('ALL')
  const [assessmentId, setAssessmentId] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [viewing, setViewing] = useState<MarkResponse | null>(null)
  const marks = useQuery({
    queryKey: enrollmentId !== 'ALL' ? markKeys.byEnrollment(enrollmentId) : assessmentId !== 'ALL' ? markKeys.byAssessment(assessmentId) : markKeys.all,
    queryFn: () => enrollmentId !== 'ALL' ? getMarksByEnrollment(Number(enrollmentId)) : assessmentId !== 'ALL' ? getMarksByAssessment(Number(assessmentId)) : getMarks(),
  })
  const enrollments = useQuery({ queryKey: ['admin', 'enrollments'], queryFn: getEnrollments })
  const assessments = useQuery({ queryKey: ['admin', 'assessments'], queryFn: getAssessments })
  const students = useQuery({ queryKey: ['admin', 'students'], queryFn: getStudents })
  const courses = useQuery({ queryKey: ['admin', 'courses'], queryFn: getCourses })
  const create = useMutation({ mutationFn: createMark, onSuccess: async () => { await client.invalidateQueries({ queryKey: ['admin'] }); setFormOpen(false); toast.success('Mark recorded') } })
  const detail = useQuery({ queryKey: markKeys.detail(viewing?.markId ?? 0), queryFn: () => getMark(viewing!.markId), enabled: viewing !== null })
  const enrollmentById = useMemo(() => new Map((enrollments.data ?? []).map((item) => [item.enrollmentId, item])), [enrollments.data])
  const assessmentById = useMemo(() => new Map((assessments.data ?? []).map((item) => [item.assessmentId, item])), [assessments.data])
  const studentById = useMemo(() => new Map((students.data ?? []).map((item) => [item.studentId, item])), [students.data])
  const courseById = useMemo(() => new Map((courses.data ?? []).map((item) => [item.courseId, item])), [courses.data])
  const filtered = useMemo(() => (marks.data ?? []).filter((mark) => {
    const enrollment = enrollmentById.get(mark.enrollmentId)
    const assessment = assessmentById.get(mark.assessmentId)
    const course = assessment ? courseById.get(assessment.courseId) : undefined
    const student = enrollment ? studentById.get(enrollment.studentId) : undefined
    const match = `${mark.markId} ${student?.enrollmentNumber ?? ''} ${assessment?.name ?? ''} ${course?.courseCode ?? ''} ${mark.marksObtained}`.toLowerCase().includes(search.trim().toLowerCase())
    return match && (assessmentId === 'ALL' || mark.assessmentId === Number(assessmentId))
  }), [marks.data, enrollmentById, assessmentById, courseById, studentById, search, assessmentId])

  async function save(request: Parameters<typeof createMark>[0]) {
    try { await create.mutateAsync(request) } catch (error) { notifyError(error, 'The mark could not be recorded.') }
  }

  const columns = [
    { key: 'student', header: 'Student / enrollment', render: (mark: MarkResponse) => { const enrollment = enrollmentById.get(mark.enrollmentId); const student = enrollment ? studentById.get(enrollment.studentId) : undefined; return <div className="identity-cell"><span className="identity-avatar identity-avatar--teal" aria-hidden="true">{student?.firstName?.slice(0, 1) ?? '—'}</span><span><strong>{student?.enrollmentNumber ?? `Enrollment #${mark.enrollmentId}`}</strong><small>{student ? `${student.firstName} ${student.lastName ?? ''}` : `Enrollment #${mark.enrollmentId}`}</small></span></div> } },
    { key: 'assessment', header: 'Assessment', render: (mark: MarkResponse) => { const assessment = assessmentById.get(mark.assessmentId); return assessment ? `${assessment.name} · ${assessment.assessmentType}` : `Assessment #${mark.assessmentId}` } },
    { key: 'marks', header: 'Marks obtained', render: (mark: MarkResponse) => <strong className="mark-value">{mark.marksObtained}{assessmentById.get(mark.assessmentId) ? <small> / {assessmentById.get(mark.assessmentId)?.maxMarks}</small> : null}</strong> },
    { key: 'entered', header: 'Entered at', mobileHidden: true, render: (mark: MarkResponse) => mark.enteredAt || <span className="muted">—</span> },
  ]

  const dependencyError = enrollments.error ?? assessments.error ?? students.error ?? courses.error
  const dependenciesPending = enrollments.isPending || assessments.isPending || students.isPending || courses.isPending
  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic operations" title="Marks" description="Review and record marks against course assessments and student enrollments." action={<Button onClick={() => setFormOpen(true)} disabled={dependenciesPending || Boolean(dependencyError) || !enrollments.data?.length || !assessments.data?.length}><Plus size={17} aria-hidden="true" /> Enter mark</Button>} />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search marks" countLabel={`${filtered.length} matching marks`} filters={(
          <><SelectField label="Enrollment" value={enrollmentId} onChange={(event) => setEnrollmentId(event.target.value)}><option value="ALL">All enrollments</option>{enrollments.data?.map((item) => <option key={item.enrollmentId} value={item.enrollmentId}>#{item.enrollmentId} · {studentById.get(item.studentId)?.enrollmentNumber ?? `Student #${item.studentId}`}</option>)}</SelectField><SelectField label="Assessment" value={assessmentId} onChange={(event) => setAssessmentId(event.target.value)}><option value="ALL">All assessments</option>{assessments.data?.map((item) => <option key={item.assessmentId} value={item.assessmentId}>{item.name}</option>)}</SelectField></>
        )} />
        {marks.isPending ? <LoadingState label="Loading marks" /> : marks.isError ? <ErrorState error={marks.error} onRetry={() => void marks.refetch()} /> : filtered.length === 0 ? <EmptyState title={marks.data.length ? 'No marks match these filters' : 'No marks recorded yet'} description={marks.data.length ? 'Try another enrollment, assessment or search term.' : 'Record a mark after an enrollment and assessment exist.'} /> : (
          <ResourceTable caption="Marks directory" columns={columns} rows={filtered} getRowKey={(mark) => mark.markId} actions={(mark) => <Button size="sm" variant="ghost" title="View mark details" aria-label={`View mark ${mark.markId}`} onClick={() => setViewing(mark)}><Eye size={16} /></Button>} />
        )}
      </Card>
      {dependencyError && <ErrorState error={dependencyError} onRetry={() => { void enrollments.refetch(); void assessments.refetch(); void students.refetch(); void courses.refetch() }} />}
      <MarkFormDialog open={formOpen} onOpenChange={setFormOpen} enrollments={enrollments.data ?? []} assessments={assessments.data ?? []} students={students.data ?? []} courses={courses.data ?? []} onSubmit={save} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`Mark #${viewing.markId}`} description="Mark entry details" notice={detail.isPending ? <LoadingState label="Refreshing mark details" /> : detail.isError ? <ErrorState error={detail.error} onRetry={() => void detail.refetch()} /> : undefined} fields={[
        { label: 'Mark ID', value: detail.data?.markId ?? viewing.markId }, { label: 'Enrollment ID', value: detail.data?.enrollmentId ?? viewing.enrollmentId },
        { label: 'Assessment', value: assessmentById.get(detail.data?.assessmentId ?? viewing.assessmentId)?.name ?? `Assessment #${viewing.assessmentId}` },
        { label: 'Marks obtained', value: detail.data?.marksObtained ?? viewing.marksObtained }, { label: 'Entered at', value: detail.data?.enteredAt ?? viewing.enteredAt },
      ]} />}
    </div>
  )
}
