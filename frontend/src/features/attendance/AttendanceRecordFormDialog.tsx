import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import type { AttendanceSessionResponse, CourseResponse, EnrollmentResponse, StudentResponse } from '../../types/api'
import type { AttendanceRecordCreateRequest } from './attendance.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'

const schema = z.object({ attendanceSessionId: z.string().min(1, 'Select a session'), enrollmentId: z.string().min(1, 'Select an enrollment'), status: z.enum(['PRESENT', 'ABSENT', 'LATE']) })
type Values = z.infer<typeof schema>

export function AttendanceRecordFormDialog({ open, onOpenChange, sessions, enrollments, students, courses, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  sessions: AttendanceSessionResponse[]
  enrollments: EnrollmentResponse[]
  students: StudentResponse[]
  courses: CourseResponse[]
  onSubmit: (request: AttendanceRecordCreateRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, control, setError, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { attendanceSessionId: '', enrollmentId: '', status: 'PRESENT' } })
  const sessionId = useWatch({ control, name: 'attendanceSessionId' })
  const selectedSession = sessions.find((session) => session.attendanceSessionId === Number(sessionId))
  const compatibleEnrollments = enrollments.filter((enrollment) => !selectedSession || enrollment.courseId === selectedSession.courseId)
  useEffect(() => reset({ attendanceSessionId: '', enrollmentId: '', status: 'PRESENT' }), [open, reset])
  const submit = handleSubmit(async (values) => {
    const session = sessions.find((item) => item.attendanceSessionId === Number(values.attendanceSessionId))
    const enrollment = enrollments.find((item) => item.enrollmentId === Number(values.enrollmentId))
    if (session && enrollment && session.courseId !== enrollment.courseId) {
      setError('enrollmentId', { message: 'Select an enrollment for this session’s course.' })
      return
    }
    await onSubmit({ attendanceSessionId: Number(values.attendanceSessionId), enrollmentId: Number(values.enrollmentId), status: values.status })
  })
  const courseById = new Map(courses.map((course) => [course.courseId, course]))
  const studentById = new Map(students.map((student) => [student.studentId, student]))
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Record attendance" description="Create one attendance record for one enrollment in a session." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || !sessions.length || !compatibleEnrollments.length}>{isSubmitting ? 'Saving…' : 'Save attendance'}</Button></>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Attendance session" {...register('attendanceSessionId')} error={errors.attendanceSessionId?.message}><option value="">Select session</option>{sessions.map((session) => <option key={session.attendanceSessionId} value={session.attendanceSessionId}>{session.sessionDate} · {courseById.get(session.courseId)?.courseCode ?? `Course #${session.courseId}`}{session.topic ? ` · ${session.topic}` : ''}</option>)}</SelectField>
        <SelectField label="Enrollment" {...register('enrollmentId')} error={errors.enrollmentId?.message}><option value="">Select enrollment</option>{compatibleEnrollments.map((enrollment) => {
          const student = studentById.get(enrollment.studentId)
          const course = courseById.get(enrollment.courseId)
          return <option key={enrollment.enrollmentId} value={enrollment.enrollmentId}>#{enrollment.enrollmentId} · {student?.enrollmentNumber ?? `Student #${enrollment.studentId}`} · {course?.courseCode ?? `Course #${enrollment.courseId}`}</option>
        })}</SelectField>
        <SelectField label="Attendance status" {...register('status')} error={errors.status?.message}><option value="PRESENT">Present</option><option value="ABSENT">Absent</option><option value="LATE">Late</option></SelectField>
        <button className="sr-only" type="submit">Save attendance record</button>
      </form>
      {!sessions.length && <p className="form-hint">Create an attendance session first.</p>}
      {selectedSession && !compatibleEnrollments.length && <p className="form-hint">No enrollment exists for the selected session’s course.</p>}
    </Dialog>
  )
}
