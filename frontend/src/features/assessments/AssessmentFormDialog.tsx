import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { AssessmentType, CourseResponse } from '../../types/api'
import type { AssessmentCreateRequest } from './assessments.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({ courseId: z.string().min(1, 'Select a course'), name: z.string().trim().min(1, 'Name is required').max(100), assessmentType: z.enum(['QUIZ', 'ASSIGNMENT', 'MIDTERM', 'FINAL', 'PROJECT']), maxMarks: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a positive value with up to two decimals').refine((value) => Number(value) > 0, 'Maximum marks must be greater than zero'), weight: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a percentage with up to two decimals').refine((value) => Number(value) >= 0 && Number(value) <= 100, 'Weight must be between 0 and 100') })
type Values = z.infer<typeof schema>

export function AssessmentFormDialog({ open, onOpenChange, courses, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  courses: CourseResponse[]
  onSubmit: (request: AssessmentCreateRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { courseId: '', name: '', assessmentType: 'QUIZ', maxMarks: '', weight: '' } })
  useEffect(() => reset({ courseId: '', name: '', assessmentType: 'QUIZ', maxMarks: '', weight: '' }), [open, reset])
  const submit = handleSubmit(async (values) => onSubmit({ courseId: Number(values.courseId), name: values.name, assessmentType: values.assessmentType as AssessmentType, maxMarks: values.maxMarks, weight: values.weight }))
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Add assessment" description="Assessments are created against a course. The current API does not accept an assessment date." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || !courses.length}>{isSubmitting ? 'Saving…' : 'Create assessment'}</Button></>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Course" {...register('courseId')} error={errors.courseId?.message}><option value="">Select a course</option>{courses.map((course) => <option key={course.courseId} value={course.courseId}>{course.courseCode} · {course.courseName}</option>)}</SelectField>
        <InputField label="Assessment name" {...register('name')} error={errors.name?.message} />
        <div className="admin-form--two-column">
          <SelectField label="Type" {...register('assessmentType')} error={errors.assessmentType?.message}><option value="QUIZ">Quiz</option><option value="ASSIGNMENT">Assignment</option><option value="MIDTERM">Midterm</option><option value="FINAL">Final</option><option value="PROJECT">Project</option></SelectField>
          <InputField label="Maximum marks" type="number" min="0.01" step="0.01" {...register('maxMarks')} error={errors.maxMarks?.message} />
          <InputField label="Weight (%)" type="number" min="0" max="100" step="0.01" {...register('weight')} error={errors.weight?.message} />
        </div>
        <button className="sr-only" type="submit">Create assessment</button>
      </form>
      {!courses.length && <p className="form-hint">Create an active course first.</p>}
    </Dialog>
  )
}
