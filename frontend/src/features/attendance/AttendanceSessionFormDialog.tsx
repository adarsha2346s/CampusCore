import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { CourseResponse, FacultyResponse } from '../../types/api'
import type { AttendanceSessionCreateRequest } from './attendance.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({ courseId: z.string().min(1, 'Select a course'), facultyId: z.string().min(1, 'Select a faculty member'), sessionDate: z.string().min(1, 'Session date is required'), topic: z.string().trim().min(1, 'Topic is required').max(255) })
type Values = z.infer<typeof schema>

export function AttendanceSessionFormDialog({ open, onOpenChange, courses, faculty, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  courses: CourseResponse[]
  faculty: FacultyResponse[]
  onSubmit: (request: AttendanceSessionCreateRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { courseId: '', facultyId: '', sessionDate: '', topic: '' } })
  useEffect(() => reset({ courseId: '', facultyId: '', sessionDate: '', topic: '' }), [open, reset])
  const submit = handleSubmit(async (values) => onSubmit({ courseId: Number(values.courseId), facultyId: Number(values.facultyId), sessionDate: values.sessionDate, topic: values.topic }))
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Create attendance session" description="A course can have at most one session per date according to the backend data constraint." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || !courses.length || !faculty.length}>{isSubmitting ? 'Saving…' : 'Create session'}</Button></>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Course" {...register('courseId')} error={errors.courseId?.message}><option value="">Select course</option>{courses.map((course) => <option key={course.courseId} value={course.courseId}>{course.courseCode} · {course.courseName}</option>)}</SelectField>
        <SelectField label="Faculty" {...register('facultyId')} error={errors.facultyId?.message}><option value="">Select faculty</option>{faculty.map((member) => <option key={member.facultyId} value={member.facultyId}>{member.employeeNumber} · {member.firstName} {member.lastName ?? ''}</option>)}</SelectField>
        <InputField label="Session date" type="date" {...register('sessionDate')} error={errors.sessionDate?.message} />
        <InputField label="Topic" {...register('topic')} error={errors.topic?.message} />
        <button className="sr-only" type="submit">Create session</button>
      </form>
      {!courses.length && <p className="form-hint">An active course is required before creating a session.</p>}
      {!faculty.length && <p className="form-hint">A faculty profile is required before creating a session.</p>}
    </Dialog>
  )
}
