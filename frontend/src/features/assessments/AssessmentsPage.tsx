import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { PaginationControls } from '../../components/data-display/PaginationControls'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { notifyError } from '../admin-shared/feedback'
import { formatDate } from '../../lib/format/datetime'
import { courseKeys, getCourses } from '../courses/courses.api'
import { AssessmentFormDialog } from './AssessmentFormDialog'
import { assessmentKeys, createAssessment, getAssessment, getAssessmentsPage, getAssessmentsByCoursePage } from './assessments.api'
import type { AssessmentResponse } from '../../types/api'

export function AssessmentsPage() {
  const client = useQueryClient()
  const courses = useQuery({ queryKey: courseKeys.all, queryFn: getCourses })
  const [page, setPage] = useState(0)
  const pageSize = 20
  const [search, setSearch] = useState('')
  const [courseId, setCourseId] = useState('ALL')
  const [type, setType] = useState('ALL')
  const [formOpen, setFormOpen] = useState(false)
  const [viewing, setViewing] = useState<AssessmentResponse | null>(null)
  const assessments = useQuery({
    queryKey: assessmentKeys.page(page, pageSize, courseId === 'ALL' ? undefined : Number(courseId)),
    queryFn: () => courseId === 'ALL' ? getAssessmentsPage(page, pageSize) : getAssessmentsByCoursePage(Number(courseId), page, pageSize),
  })
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  const create = useMutation({ mutationFn: createAssessment, onSuccess: async () => { await refresh(); setFormOpen(false); toast.success('Assessment created') } })
  const detail = useQuery({ queryKey: assessmentKeys.detail(viewing?.assessmentId ?? 0), queryFn: () => getAssessment(viewing!.assessmentId), enabled: viewing !== null })
  const courseById = useMemo(() => new Map((courses.data ?? []).map((course) => [course.courseId, course])), [courses.data])
  const filtered = useMemo(() => (assessments.data?.content ?? []).filter((assessment) => `${assessment.name} ${assessment.assessmentType} ${courseById.get(assessment.courseId)?.courseCode ?? ''} ${courseById.get(assessment.courseId)?.courseName ?? ''}`.toLowerCase().includes(search.trim().toLowerCase()) && (type === 'ALL' || assessment.assessmentType === type)), [assessments.data, courseById, search, type])

  async function save(request: Parameters<typeof createAssessment>[0]) {
    try { await create.mutateAsync(request) } catch (error) { notifyError(error, 'The assessment could not be created.') }
  }

  const columns = [
    { key: 'name', header: 'Assessment', render: (assessment: AssessmentResponse) => <div className="identity-cell"><span className="entity-icon entity-icon--blue"><span aria-hidden="true">A</span></span><span><strong>{assessment.name}</strong><small>{assessment.assessmentType.replaceAll('_', ' ')}</small></span></div> },
    { key: 'course', header: 'Course', render: (assessment: AssessmentResponse) => { const course = courseById.get(assessment.courseId); return course ? `${course.courseCode} · ${course.courseName}` : `Course #${assessment.courseId}` } },
    { key: 'max', header: 'Max marks', render: (assessment: AssessmentResponse) => assessment.maxMarks },
    { key: 'weight', header: 'Weight', render: (assessment: AssessmentResponse) => `${assessment.weight}%` },
    { key: 'date', header: 'Date', mobileHidden: true, render: (assessment: AssessmentResponse) => formatDate(assessment.assessmentDate) ?? <span className="muted">Not set</span> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic operations" title="Assessments" description="Define course assessment components and their contribution to grading." action={<Button onClick={() => setFormOpen(true)} disabled={courses.isPending || courses.isError || !courses.data?.some((course) => course.status === 'ACTIVE')}><Plus size={17} aria-hidden="true" /> Add assessment</Button>} />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={(value) => { setSearch(value); setPage(0) }} searchLabel="Search this page" countLabel={`${filtered.length} shown · ${assessments.data?.totalElements ?? 0} total`} filters={(
          <><SelectField label="Course" value={courseId} onChange={(event) => { setCourseId(event.target.value); setPage(0) }}><option value="ALL">All courses</option>{courses.data?.map((course) => <option key={course.courseId} value={course.courseId}>{course.courseCode}</option>)}</SelectField><SelectField label="Type" value={type} onChange={(event) => { setType(event.target.value); setPage(0) }}><option value="ALL">All types</option><option value="QUIZ">Quiz</option><option value="ASSIGNMENT">Assignment</option><option value="MIDTERM">Midterm</option><option value="FINAL">Final</option><option value="PROJECT">Project</option></SelectField></>
        )} />
        {assessments.isPending ? <TableSkeleton rows={8} columns={5} label="Loading assessments" /> : assessments.isError ? <ErrorState error={assessments.error} onRetry={() => void assessments.refetch()} /> : filtered.length === 0 ? <EmptyState title={assessments.data.totalElements ? 'No assessments on this page match' : 'No assessments yet'} description={assessments.data.totalElements ? 'Try another search, course, type or page.' : 'Add an assessment to a course to build its grading structure.'} /> : (
          <ResourceTable caption="Assessment directory" columns={columns} rows={filtered} getRowKey={(assessment) => assessment.assessmentId} actions={(assessment) => <Button size="sm" variant="ghost" title="View assessment details" aria-label={`View ${assessment.name}`} onClick={() => setViewing(assessment)}><Eye size={16} /></Button>} />
        )}
        {!assessments.isPending && !assessments.isError && assessments.data && <PaginationControls page={assessments.data.page} size={assessments.data.size} totalElements={assessments.data.totalElements} totalPages={assessments.data.totalPages} onPageChange={setPage} />}
      </Card>
      {courses.isError && <ErrorState error={courses.error} onRetry={() => void courses.refetch()} />}
      <AssessmentFormDialog open={formOpen} onOpenChange={setFormOpen} courses={(courses.data ?? []).filter((course) => course.status === 'ACTIVE')} onSubmit={save} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={detail.data?.name ?? viewing.name} description="Assessment details" notice={detail.isPending ? <LoadingState label="Refreshing assessment details" /> : detail.isError ? <ErrorState error={detail.error} onRetry={() => void detail.refetch()} /> : undefined} fields={[
        { label: 'Assessment ID', value: detail.data?.assessmentId ?? viewing.assessmentId }, { label: 'Type', value: detail.data?.assessmentType ?? viewing.assessmentType },
        { label: 'Course', value: courseById.get(detail.data?.courseId ?? viewing.courseId)?.courseCode ?? `Course #${viewing.courseId}` },
        { label: 'Maximum marks', value: detail.data?.maxMarks ?? viewing.maxMarks }, { label: 'Weight', value: `${detail.data?.weight ?? viewing.weight}%` },
        { label: 'Assessment date', value: formatDate(detail.data?.assessmentDate ?? viewing.assessmentDate) ?? 'Not set' },
      ]} />}
    </div>
  )
}
