import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Department, StudentRequest, StudentResponse, UserResponse } from '../../types/api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({
  userId: z.string().min(1, 'Choose a student account'),
  departmentId: z.string().min(1, 'Choose a department'),
  enrollmentNumber: z.string().trim().min(1, 'Enrollment number is required').max(50),
  firstName: z.string().trim().min(1, 'First name is required').max(100),
  lastName: z.string().max(100),
  dateOfBirth: z.string(),
  phone: z.string().max(20),
  admissionYear: z.string().regex(/^\d{4}$/, 'Enter a four-digit year'),
})
type Values = z.infer<typeof schema>

export function StudentFormDialog({ open, onOpenChange, student, users, departments, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  student: StudentResponse | null
  users: UserResponse[]
  departments: Department[]
  onSubmit: (request: StudentRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema), defaultValues: { userId: '', departmentId: '', enrollmentNumber: '', firstName: '', lastName: '', dateOfBirth: '', phone: '', admissionYear: '' },
  })
  useEffect(() => reset({
    userId: student ? String(student.userId) : '', departmentId: student ? String(student.departmentId) : '',
    enrollmentNumber: student?.enrollmentNumber ?? '', firstName: student?.firstName ?? '', lastName: student?.lastName ?? '',
    dateOfBirth: student?.dateOfBirth ?? '', phone: student?.phone ?? '', admissionYear: student ? String(student.admissionYear) : '',
  }), [student, open, reset])
  const submit = handleSubmit(async (values) => onSubmit({
    userId: Number(values.userId), departmentId: Number(values.departmentId), enrollmentNumber: values.enrollmentNumber,
    firstName: values.firstName, lastName: values.lastName || null, dateOfBirth: values.dateOfBirth || null,
    phone: values.phone || null, admissionYear: Number(values.admissionYear),
  }))

  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={student ? 'Edit student record' : 'Add student'} description="Link an existing STUDENT account to an academic profile." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || departments.length === 0 || users.length === 0}>{isSubmitting ? 'Saving…' : 'Save student'}</Button></>
    )}>
      <form className="admin-form admin-form--two-column" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Student account" {...register('userId')} error={errors.userId?.message}>
          <option value="">Select account</option>{users.map((user) => <option key={user.userId} value={user.userId}>{user.username} · {user.email}</option>)}
        </SelectField>
        <SelectField label="Department" {...register('departmentId')} error={errors.departmentId?.message}>
          <option value="">Select department</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.code} · {department.name}</option>)}
        </SelectField>
        <InputField label="Enrollment number" {...register('enrollmentNumber')} error={errors.enrollmentNumber?.message} />
        <InputField label="First name" {...register('firstName')} error={errors.firstName?.message} />
        <InputField label="Last name" {...register('lastName')} error={errors.lastName?.message} />
        <InputField label="Admission year" inputMode="numeric" {...register('admissionYear')} error={errors.admissionYear?.message} />
        <InputField label="Date of birth" type="date" {...register('dateOfBirth')} error={errors.dateOfBirth?.message} />
        <InputField label="Phone" type="tel" {...register('phone')} error={errors.phone?.message} />
        <button className="sr-only" type="submit">Save student</button>
      </form>
      {departments.length === 0 && <p className="form-hint">Create a department before adding a student.</p>}
      {users.length === 0 && <p className="form-hint">Create an active STUDENT user account first.</p>}
    </Dialog>
  )
}
