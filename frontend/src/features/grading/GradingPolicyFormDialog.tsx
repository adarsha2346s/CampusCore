import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { GradingPolicyRequest } from '../../types/api'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const decimal = /^\d+(\.\d{1,2})?$/
const schema = z.object({ name: z.string().trim().min(1, 'Policy name is required').max(100), minPercentage: z.string().regex(decimal, 'Enter a number with up to two decimals').refine((value) => Number(value) >= 0 && Number(value) <= 100, 'Percentage must be from 0 to 100'), maxPercentage: z.string().regex(decimal, 'Enter a number with up to two decimals').refine((value) => Number(value) >= 0 && Number(value) <= 100, 'Percentage must be from 0 to 100'), grade: z.string().trim().min(1, 'Grade is required').max(5), gradePoint: z.string().regex(decimal, 'Enter a number with up to two decimals').refine((value) => Number(value) >= 0, 'Grade point cannot be negative') })
type Values = z.infer<typeof schema>

export function GradingPolicyFormDialog({ open, onOpenChange, onSubmit }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (request: GradingPolicyRequest) => Promise<void>
}) {
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: '', minPercentage: '', maxPercentage: '', grade: '', gradePoint: '' } })
  useEffect(() => reset({ name: '', minPercentage: '', maxPercentage: '', grade: '', gradePoint: '' }), [open, reset])
  const submit = handleSubmit(async (values) => {
    if (Number(values.minPercentage) > Number(values.maxPercentage)) {
      setError('maxPercentage', { message: 'Maximum percentage must be at least the minimum.' })
      return
    }
    await onSubmit({ name: values.name, minPercentage: Number(values.minPercentage), maxPercentage: Number(values.maxPercentage), grade: values.grade, gradePoint: Number(values.gradePoint) })
  })
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Create grading policy" description="Add one grade band to a named grading policy." footer={(
      <><Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>Cancel</Button><Button onClick={() => void submit()} disabled={isSubmitting}>{isSubmitting ? 'Saving…' : 'Create policy'}</Button></>
    )}>
      <form className="admin-form" onSubmit={(event) => { event.preventDefault(); void submit() }}>
        <InputField label="Policy name" {...register('name')} error={errors.name?.message} />
        <div className="admin-form--two-column"><InputField label="Minimum percentage" type="number" min="0" max="100" step="0.01" {...register('minPercentage')} error={errors.minPercentage?.message} /><InputField label="Maximum percentage" type="number" min="0" max="100" step="0.01" {...register('maxPercentage')} error={errors.maxPercentage?.message} /><InputField label="Grade" {...register('grade')} error={errors.grade?.message} /><InputField label="Grade point" type="number" min="0" step="0.01" {...register('gradePoint')} error={errors.gradePoint?.message} /></div>
        <button className="sr-only" type="submit">Create grading policy</button>
      </form>
    </Dialog>
  )
}
