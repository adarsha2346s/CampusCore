import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Department, DepartmentRequest } from '../../types/api'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100),
  code: z.string().trim().min(1, 'Code is required').max(20),
})
type Values = z.infer<typeof schema>

export function DepartmentFormDialog({ open, onOpenChange, department, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  department: Department | null
  onSubmit: (request: DepartmentRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema), defaultValues: { name: '', code: '' },
  })
  useEffect(() => reset({ name: department?.name ?? '', code: department?.code ?? '' }), [department, open, reset])
  const submit = handleSubmit(async (values) => onSubmit(values))
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={department ? 'Edit department' : 'Add department'} description="Department names and codes must be unique." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting}>{isSubmitting ? 'Saving…' : 'Save department'}</Button></>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <InputField label="Department name" {...register('name')} error={errors.name?.message} />
        <InputField label="Department code" {...register('code')} error={errors.code?.message} />
        <button className="sr-only" type="submit">Save department</button>
      </form>
    </Dialog>
  )
}
