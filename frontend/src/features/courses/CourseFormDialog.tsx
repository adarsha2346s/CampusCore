import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { CourseRequest, CourseResponse, Department } from '../../types/api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({
  departmentId: z.string().min(1, 'Choose a department'),
  courseCode: z.string().trim().min(1, 'Course code is required').max(30),
  courseName: z.string().trim().min(1, 'Course name is required').max(150),
  credits: z.string().regex(/^[1-9]\d*$/, 'Enter at least 1 credit'),
  capacity: z.string().regex(/^[1-9]\d*$/, 'Enter a capacity of at least 1'),
})
type Values = z.infer<typeof schema>

export function CourseFormDialog({ open, onOpenChange, course, departments, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  course: CourseResponse | null
  departments: Department[]
  onSubmit: (request: CourseRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema), defaultValues: { departmentId: '', courseCode: '', courseName: '', credits: '', capacity: '' },
  })
  useEffect(() => reset({
    departmentId: course ? String(course.departmentId) : '', courseCode: course?.courseCode ?? '',
    courseName: course?.courseName ?? '', credits: course ? String(course.credits) : '', capacity: course ? String(course.capacity) : '',
  }), [course, open, reset])
  const submit = handleSubmit(async (values) => onSubmit({
    departmentId: Number(values.departmentId), courseCode: values.courseCode, courseName: values.courseName,
    credits: Number(values.credits), capacity: Number(values.capacity),
  }))
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={course ? 'Edit course' : 'Add course'} description="Configure the course catalog entry and capacity." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || departments.length === 0}>{isSubmitting ? 'Saving…' : 'Save course'}</Button></>
    )}>
      <form className="admin-form admin-form--two-column" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Department" {...register('departmentId')} error={errors.departmentId?.message}>
          <option value="">Select department</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.code} · {department.name}</option>)}
        </SelectField>
        <InputField label="Course code" {...register('courseCode')} error={errors.courseCode?.message} />
        <InputField label="Course name" {...register('courseName')} error={errors.courseName?.message} />
        <InputField label="Credits" type="number" min="1" step="1" {...register('credits')} error={errors.credits?.message} />
        <InputField label="Capacity" type="number" min="1" step="1" {...register('capacity')} error={errors.capacity?.message} />
        <button className="sr-only" type="submit">Save course</button>
      </form>
      {departments.length === 0 && <p className="form-hint">Create a department before adding a course.</p>}
    </Dialog>
  )
}
