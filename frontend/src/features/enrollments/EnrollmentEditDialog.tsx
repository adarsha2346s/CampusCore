import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { EnrollmentResponse } from '../../types/api'
import type { EnrollmentUpdateRequest } from './enrollments.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({
  semester: z.string().trim().min(1, 'Semester is required').max(30),
  academicYear: z.string().trim().min(1, 'Academic year is required').max(20),
  status: z.enum(['ENROLLED', 'DROPPED', 'COMPLETED']),
})
type Values = z.infer<typeof schema>

/**
 * Edit an enrollment's term and lifecycle status.
 *
 * Status is the intended archive control. The term can be corrected while the
 * enrollment has no marks or attendance; afterwards the server refuses a term
 * change and the notice below explains why rather than letting the admin try.
 */
export function EnrollmentEditDialog({
  open,
  onOpenChange,
  enrollment,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  enrollment: EnrollmentResponse | null
  onSubmit: (enrollmentId: number, request: EnrollmentUpdateRequest) => Promise<void>
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { semester: '', academicYear: '', status: 'ENROLLED' },
  })

  useEffect(
    () =>
      reset({
        semester: enrollment?.semester ?? '',
        academicYear: enrollment?.academicYear ?? '',
        status: enrollment?.status ?? 'ENROLLED',
      }),
    [enrollment, open, reset],
  )

  const submit = handleSubmit(async (values) => {
    if (!enrollment) return
    await onSubmit(enrollment.enrollmentId, values)
  })

  const locked = enrollment?.status === 'COMPLETED'

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={enrollment ? `Edit enrollment #${enrollment.enrollmentId}` : 'Edit enrollment'}
      description="Record the outcome of a term, or correct the term while no marks or attendance exist."
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={() => void submit()} disabled={isSubmitting || !enrollment}>
            {isSubmitting ? 'Saving…' : 'Save enrollment'}
          </Button>
        </>
      }
    >
      <form
        className="admin-form"
        onSubmit={(event) => {
          event.preventDefault()
          void submit()
        }}
      >
        <div className="admin-form--two-column">
          <InputField
            label="Semester"
            placeholder="For example, Fall"
            {...register('semester')}
            error={errors.semester?.message}
          />
          <InputField
            label="Academic year"
            placeholder="Use the institution’s year format"
            {...register('academicYear')}
            error={errors.academicYear?.message}
          />
        </div>

        <SelectField label="Status" {...register('status')} error={errors.status?.message}>
          <option value="ENROLLED">Enrolled</option>
          <option value="DROPPED">Dropped</option>
          <option value="COMPLETED">Completed</option>
        </SelectField>

        {locked && (
          <p className="form-hint">
            This enrollment is already completed. Change the status back to enrolled only if that was
            recorded in error.
          </p>
        )}

        <p className="form-hint">
          Once marks or attendance exist for this enrollment, its term can no longer be changed.
          Use the status to record the outcome instead.
        </p>

        <button className="sr-only" type="submit">
          Save enrollment
        </button>
      </form>
    </Dialog>
  )
}
