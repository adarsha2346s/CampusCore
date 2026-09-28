import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { UserRequest, UserResponse } from '../../types/api'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'
import { SelectField } from '../../components/forms/SelectField'

const schema = z.object({
  username: z.string().trim().min(1, 'Username is required').max(50),
  email: z.string().trim().email('Enter a valid email').max(150),
  password: z.string().min(6, 'Password must be at least 6 characters').max(100),
  role: z.enum(['ADMIN', 'FACULTY', 'STUDENT']),
})
type Values = z.infer<typeof schema>

interface UserFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  user: UserResponse | null
  onSubmit: (request: UserRequest) => Promise<void>
}

export function UserFormDialog({ open, onOpenChange, user, onSubmit }: UserFormDialogProps) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', email: '', password: '', role: 'STUDENT' },
  })

  useEffect(() => {
    reset({ username: user?.username ?? '', email: user?.email ?? '', password: '', role: user?.role ?? 'STUDENT' })
  }, [user, open, reset])

  const submit = handleSubmit(async (values) => onSubmit(values as UserRequest))

  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={user ? 'Edit user account' : 'Create user account'} description={user ? 'Saving this form also sets a new password, as required by the API.' : 'Create a campus account with a starting password and role.'} footer={(
      <>
        <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button>
        <Button onClick={() => void submit()} disabled={isSubmitting}>{isSubmitting ? 'Saving…' : user ? 'Save changes' : 'Create account'}</Button>
      </>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <InputField label="Username" autoComplete="off" {...register('username')} error={errors.username?.message} />
        <InputField label="Email" type="email" autoComplete="off" {...register('email')} error={errors.email?.message} />
        <InputField label={user ? 'New password (required by API)' : 'Temporary password'} type="password" autoComplete="new-password" {...register('password')} error={errors.password?.message} />
        <SelectField label="Role" {...register('role')} error={errors.role?.message}>
          <option value="ADMIN">Administrator</option>
          <option value="FACULTY">Faculty</option>
          <option value="STUDENT">Student</option>
        </SelectField>
        <button className="sr-only" type="submit">Save account</button>
      </form>
    </Dialog>
  )
}
