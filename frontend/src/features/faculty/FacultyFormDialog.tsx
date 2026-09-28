import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Department, UserResponse } from '../../types/api'
import type { FacultyCreateRequest } from './faculty.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({
  userId: z.string().min(1, 'Choose a faculty account'),
  departmentId: z.string().min(1, 'Choose a department'),
  employeeNumber: z.string().trim().min(1, 'Employee number is required').max(50),
  firstName: z.string().trim().min(1, 'First name is required').max(100),
  lastName: z.string().max(100),
  phone: z.string().max(20),
})
type Values = z.infer<typeof schema>

export function FacultyFormDialog({ open, onOpenChange, users, departments, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  users: UserResponse[]
  departments: Department[]
  onSubmit: (request: FacultyCreateRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema), defaultValues: { userId: '', departmentId: '', employeeNumber: '', firstName: '', lastName: '', phone: '' },
  })
  useEffect(() => reset({ userId: '', departmentId: '', employeeNumber: '', firstName: '', lastName: '', phone: '' }), [open, reset])
  const submit = handleSubmit(async (values) => {
    const request: FacultyCreateRequest = {
      userId: Number(values.userId), departmentId: Number(values.departmentId), employeeNumber: values.employeeNumber,
      firstName: values.firstName, ...(values.lastName ? { lastName: values.lastName } : {}), ...(values.phone ? { phone: values.phone } : {}),
    }
    await onSubmit(request)
  })
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Add faculty profile" description="Faculty account creation and profile creation are separate API operations. Select an existing FACULTY user." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting || users.length === 0 || departments.length === 0}>{isSubmitting ? 'Saving…' : 'Create faculty profile'}</Button></>
    )}>
      <form className="admin-form admin-form--two-column" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <SelectField label="Faculty account" {...register('userId')} error={errors.userId?.message}><option value="">Select account</option>{users.map((user) => <option key={user.userId} value={user.userId}>{user.username} · {user.email}</option>)}</SelectField>
        <SelectField label="Department" {...register('departmentId')} error={errors.departmentId?.message}><option value="">Select department</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.code} · {department.name}</option>)}</SelectField>
        <InputField label="Employee number" {...register('employeeNumber')} error={errors.employeeNumber?.message} />
        <InputField label="First name" {...register('firstName')} error={errors.firstName?.message} />
        <InputField label="Last name" {...register('lastName')} error={errors.lastName?.message} />
        <InputField label="Phone" type="tel" {...register('phone')} error={errors.phone?.message} />
        <button className="sr-only" type="submit">Create faculty</button>
      </form>
      {users.length === 0 && <p className="form-hint">Create an active FACULTY user account first.</p>}
      {departments.length === 0 && <p className="form-hint">Create a department before adding faculty.</p>}
    </Dialog>
  )
}
