import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { CourseResponse, StudentResponse } from '../../types/api'
import type { EnrollmentCreateRequest } from './enrollments.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({ studentId: z.string().min(1, 'Select a student'), courseId: z.string().min(1, 'Select a course'), semester: z.string().trim().min(1, 'Semester is required').max(30), academicYear: z.string().trim().min(1, 'Academic year is required').max(20) })
type Values = z.infer<typeof schema>

export function EnrollmentFormDialog({ open, onOpenChange, students, courses, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  students: StudentResponse[]
  courses: CourseResponse[]
  onSubmit: (request: EnrollmentCreateRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { studentId: '', courseId: '', semester: '', academicYear: '' } })
  useEffect(() => reset({ studentId: '', courseId: '', semester: '', academicYear: '' }), [open, reset])
  const submit = handleSubmit(async (values) => onSubmit({ studentId: Number(values.studentId), courseId: Number(values.courseId), semester: values.semester, academicYear: values.academicYear }))
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Create enrollment" description="Link an active student profile to an active course offering." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || !students.length || !courses.length}>{isSubmitting ? 'Saving…' : 'Create enrollment'}</Button></>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Student" {...register('studentId')} error={errors.studentId?.message}><option value="">Select a student</option>{students.map((student) => <option key={student.studentId} value={student.studentId}>{student.enrollmentNumber} · {student.firstName} {student.lastName ?? ''}</option>)}</SelectField>
        <SelectField label="Course" {...register('courseId')} error={errors.courseId?.message}><option value="">Select a course</option>{courses.map((course) => <option key={course.courseId} value={course.courseId}>{course.courseCode} · {course.courseName}</option>)}</SelectField>
        <div className="admin-form--two-column"><InputField label="Semester" placeholder="For example, Fall" {...register('semester')} error={errors.semester?.message} /><InputField label="Academic year" placeholder="Use the institution’s year format" {...register('academicYear')} error={errors.academicYear?.message} /></div>
        <button className="sr-only" type="submit">Create enrollment</button>
      </form>
      {students.length === 0 && <p className="form-hint">An active student profile is required before enrollment.</p>}
      {courses.length === 0 && <p className="form-hint">An active course is required before enrollment.</p>}
    </Dialog>
  )
}
