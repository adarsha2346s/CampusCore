import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BookOpen, Eye, Plus, Search } from 'lucide-react'
import { toast } from 'sonner'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { SelectField } from '../../components/forms/SelectField'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { PageHeader } from '../../components/ui/PageHeader'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { notifyError } from '../admin-shared/feedback'
import { getEnrollments, enrollmentKeys } from '../enrollments/enrollments.api'
import { GradingPolicyFormDialog } from './GradingPolicyFormDialog'
import { createGradingPolicy, getEnrollmentGpa, getGradingPolicies, gradingPolicyKeys, gpaKeys } from './grading.api'
import type { GradingPolicy } from '../../types/api'

const lookupSchema = z.object({ enrollmentId: z.string().min(1, 'Choose an enrollment'), policyName: z.string() })
type LookupValues = z.infer<typeof lookupSchema>

export function GradingPage() {
  const client = useQueryClient()
  const policies = useQuery({ queryKey: gradingPolicyKeys.all, queryFn: getGradingPolicies })
  const enrollments = useQuery({ queryKey: enrollmentKeys.all, queryFn: getEnrollments })
  const [search, setSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [viewing, setViewing] = useState<GradingPolicy | null>(null)
  const [lookup, setLookup] = useState<LookupValues | null>(null)
  const create = useMutation({ mutationFn: createGradingPolicy, onSuccess: async () => { await client.invalidateQueries({ queryKey: ['admin'] }); setFormOpen(false); toast.success('Grading policy created') } })
  const gpa = useQuery({
    queryKey: gpaKeys.enrollment(lookup?.enrollmentId ?? '', lookup?.policyName ?? ''),
    queryFn: () => getEnrollmentGpa(Number(lookup!.enrollmentId), lookup!.policyName || undefined),
    enabled: lookup !== null,
  })
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LookupValues>({ resolver: zodResolver(lookupSchema), defaultValues: { enrollmentId: '', policyName: '' } })
  const policyNames = useMemo(() => [...new Set((policies.data ?? []).map((policy) => policy.name))].sort(), [policies.data])
  const filtered = useMemo(() => (policies.data ?? []).filter((policy) => `${policy.name} ${policy.grade}`.toLowerCase().includes(search.trim().toLowerCase())), [policies.data, search])
  async function savePolicy(request: Parameters<typeof createGradingPolicy>[0]) {
    try { await create.mutateAsync(request) } catch (error) { notifyError(error, 'The grading policy could not be created.') }
  }
  const requestGpa = handleSubmit((values) => { setLookup(values) })
  const columns = [
    { key: 'policy', header: 'Policy', render: (policy: GradingPolicy) => <div className="identity-cell"><span className="entity-icon entity-icon--blue"><BookOpen size={17} aria-hidden="true" /></span><span><strong>{policy.name}</strong><small>Grade band</small></span></div> },
    { key: 'range', header: 'Percentage band', render: (policy: GradingPolicy) => `${policy.minPercentage}% – ${policy.maxPercentage}%` },
    { key: 'grade', header: 'Grade', render: (policy: GradingPolicy) => <Badge className="grade-badge">{policy.grade}</Badge> },
    { key: 'point', header: 'Grade point', render: (policy: GradingPolicy) => policy.gradePoint },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic operations" title="Grading" description="Review grading policy bands and request course GPA calculations from the backend." action={<Button onClick={() => setFormOpen(true)}><Plus size={17} aria-hidden="true" /> Add policy band</Button>} />
      <Card className="directory-card">
        <div className="directory-toolbar"><div className="directory-toolbar__search"><Search size={17} aria-hidden="true" /><label className="sr-only" htmlFor="policy-search">Search grading policies</label><input id="policy-search" className="input" type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search policy or grade" /></div><p className="directory-toolbar__count">{filtered.length} policy bands</p></div>
        {policies.isPending ? <TableSkeleton rows={7} columns={4} label="Loading grading policies" /> : policies.isError ? <ErrorState error={policies.error} onRetry={() => void policies.refetch()} /> : !filtered.length ? <EmptyState title={policies.data.length ? 'No policy bands match this search' : 'No grading policies yet'} description={policies.data.length ? 'Try another policy name or grade.' : 'Add policy bands to support the backend GPA calculation.'} /> : <ResourceTable caption="Grading policy bands" columns={columns} rows={filtered} getRowKey={(policy) => policy.gradingPolicyId} actions={(policy) => <Button size="sm" variant="ghost" title="View policy details" aria-label={`View ${policy.name} ${policy.grade} policy band`} onClick={() => setViewing(policy)}><Eye size={16} /></Button>} />}
      </Card>

      <Card className="gpa-lookup-card">
        <div className="section-heading"><div><p className="eyebrow">Backend calculation</p><h2>Course GPA lookup</h2><p className="muted">Select an enrollment and optionally choose a policy name. The API calculates and returns the result.</p></div></div>
        {enrollments.isError ? <ErrorState error={enrollments.error} onRetry={() => void enrollments.refetch()} /> : (
          <form className="gpa-lookup-form" onSubmit={(event) => { event.preventDefault(); void requestGpa() }}>
            <SelectField label="Enrollment" {...register('enrollmentId')} error={errors.enrollmentId?.message}><option value="">Select enrollment</option>{enrollments.data?.map((enrollment) => <option key={enrollment.enrollmentId} value={enrollment.enrollmentId}>#{enrollment.enrollmentId} · Student #{enrollment.studentId} · Course #{enrollment.courseId} · {enrollment.semester} {enrollment.academicYear}</option>)}</SelectField>
            <SelectField label="Grading policy" {...register('policyName')}><option value="">Backend default (DEFAULT_10_POINT)</option>{policyNames.map((name) => <option key={name} value={name}>{name}</option>)}</SelectField>
            <Button type="submit" disabled={isSubmitting || enrollments.isPending}>{isSubmitting ? 'Loading…' : 'Calculate GPA'}</Button>
          </form>
        )}
        {enrollments.data?.length === 0 && <p className="form-hint">No enrollments are available for GPA lookup.</p>}
        {lookup && (gpa.isPending ? <LoadingState label="Requesting GPA calculation" /> : gpa.isError ? <ErrorState error={gpa.error} onRetry={() => void gpa.refetch()} /> : gpa.data && (
          <div className="gpa-result" aria-live="polite">
            <div className="gpa-result__grade"><span>Backend result</span><strong>{gpa.data.grade}</strong><small>{gpa.data.gradePoint} grade points</small></div>
            <div className="gpa-result__main"><p className="eyebrow">{gpa.data.courseCode} · {gpa.data.courseName}</p><strong>{gpa.data.currentPercentage}%</strong><span>Current course percentage</span></div>
            <div className="gpa-result__meta"><span><small>Enrollment</small><strong>#{gpa.data.enrollmentId}</strong></span><span><small>Assessed weight</small><strong>{gpa.data.assessedWeight}%</strong></span><Badge className={gpa.data.complete ? 'result-complete' : 'result-in-progress'}>{gpa.data.complete ? 'Complete' : 'In progress'}</Badge></div>
          </div>
        ))}
      </Card>

      <GradingPolicyFormDialog open={formOpen} onOpenChange={setFormOpen} onSubmit={savePolicy} />
      {viewing && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setViewing(null) }} title={`${viewing.name} · ${viewing.grade}`} description="Grading policy band" fields={[
        { label: 'Policy name', value: viewing.name }, { label: 'Grade', value: viewing.grade }, { label: 'Minimum percentage', value: `${viewing.minPercentage}%` }, { label: 'Maximum percentage', value: `${viewing.maxPercentage}%` }, { label: 'Grade point', value: viewing.gradePoint }, { label: 'Policy ID', value: viewing.gradingPolicyId },
      ]} />}
    </div>
  )
}
