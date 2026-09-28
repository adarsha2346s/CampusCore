import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import type { AssessmentResponse, CourseResponse, EnrollmentResponse, StudentResponse } from '../../types/api'
import type { MarkCreateRequest } from './marks.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({ enrollmentId: z.string().min(1, 'Select an enrollment'), assessmentId: z.string().min(1, 'Select an assessment'), marksObtained: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a non-negative value with up to two decimals') })
type Values = z.infer<typeof schema>

export function MarkFormDialog({ open, onOpenChange, enrollments, assessments, students, courses, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  enrollments: EnrollmentResponse[]
  assessments: AssessmentResponse[]
  students: StudentResponse[]
  courses: CourseResponse[]
  onSubmit: (request: MarkCreateRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, control, setError, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { enrollmentId: '', assessmentId: '', marksObtained: '' } })
  const enrollmentId = useWatch({ control, name: 'enrollmentId' })
  const selectedEnrollment = enrollments.find((enrollment) => enrollment.enrollmentId === Number(enrollmentId))
  const compatibleAssessments = assessments.filter((assessment) => !selectedEnrollment || assessment.courseId === selectedEnrollment.courseId)
  useEffect(() => reset({ enrollmentId: '', assessmentId: '', marksObtained: '' }), [open, reset])
  const submit = handleSubmit(async (values) => {
    const assessment = assessments.find((item) => item.assessmentId === Number(values.assessmentId))
    if (selectedEnrollment && assessment && assessment.courseId !== selectedEnrollment.courseId) {
      setError('assessmentId', { message: 'Choose an assessment for the course in this enrollment.' })
      return
    }
    const marks = Number(values.marksObtained)
    if (assessment && marks > assessment.maxMarks) {
      setError('marksObtained', { message: `Marks cannot exceed this assessment’s maximum of ${assessment.maxMarks}.` })
      return
    }
    await onSubmit({ enrollmentId: Number(values.enrollmentId), assessmentId: Number(values.assessmentId), marksObtained: values.marksObtained })
  })
  const courseById = new Map(courses.map((course) => [course.courseId, course]))
  const studentById = new Map(students.map((student) => [student.studentId, student]))
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Enter mark" description="Marks are recorded once per enrollment and assessment by the backend." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || !enrollments.length || !assessments.length}>{isSubmitting ? 'Saving…' : 'Save mark'}</Button></>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Enrollment" {...register('enrollmentId')} error={errors.enrollmentId?.message}><option value="">Select enrollment</option>{enrollments.map((enrollment) => {
          const student = studentById.get(enrollment.studentId)
          const course = courseById.get(enrollment.courseId)
          return <option key={enrollment.enrollmentId} value={enrollment.enrollmentId}>#{enrollment.enrollmentId} · {student?.enrollmentNumber ?? `Student #${enrollment.studentId}`} · {course?.courseCode ?? `Course #${enrollment.courseId}`}</option>
        })}</SelectField>
        <SelectField label="Assessment" {...register('assessmentId')} error={errors.assessmentId?.message}><option value="">Select assessment</option>{compatibleAssessments.map((assessment) => <option key={assessment.assessmentId} value={assessment.assessmentId}>{assessment.name} · max {assessment.maxMarks}</option>)}</SelectField>
        <InputField label="Marks obtained" type="number" min="0" step="0.01" {...register('marksObtained')} error={errors.marksObtained?.message} />
        <button className="sr-only" type="submit">Save mark</button>
      </form>
      {!enrollments.length && <p className="form-hint">Create an enrollment before entering marks.</p>}
      {!assessments.length && <p className="form-hint">Create an assessment before entering marks.</p>}
    </Dialog>
  )
}
