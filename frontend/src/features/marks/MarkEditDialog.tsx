import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import type { AssessmentResponse, MarkResponse } from '../../types/api'
import type { MarkUpdateRequest } from './marks.api'
import { InputField } from '../../components/ui/InputField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'

const schema = z.object({
  marksObtained: z
    .string()
    .regex(/^\d+(\.\d{1,2})?$/, 'Enter a value with up to two decimals')
    .refine((value) => Number(value) > 0, 'Marks must be greater than zero')
    .refine(
      (value) => value.length <= 6,
      'Marks must fit the six digit, two decimal column',
    ),
})
type Values = z.infer<typeof schema>

/**
 * Correct a recorded mark.
 *
 * The student, enrollment and assessment are shown for context but cannot be
 * changed: a mark belongs to one student's result for one assessment, and
 * re-pointing it would move a score onto the wrong row. The assessment's maximum
 * is displayed so the ceiling is visible while correcting the value, and the
 * server enforces the same limit.
 */
export function MarkEditDialog({
  open,
  onOpenChange,
  mark,
  assessment,
  context,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  mark: MarkResponse | null
  assessment: AssessmentResponse | null
  context: string
  onSubmit: (markId: number, request: MarkUpdateRequest) => Promise<void>
}) {
  const maxMarks = assessment ? Number(assessment.maxMarks) : null

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { marksObtained: '' },
  })

  useEffect(
    () => reset({ marksObtained: mark ? String(mark.marksObtained) : '' }),
    [mark, open, reset],
  )

  const submitted = useWatch({ control, name: 'marksObtained' })
  const overCeiling =
    maxMarks !== null && submitted !== '' && Number(submitted) > maxMarks

  const submit = handleSubmit(async (values) => {
    if (!mark) return
    await onSubmit(mark.markId, { marksObtained: Number(values.marksObtained) })
  })

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Correct mark"
      description={context}
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={() => void submit()} disabled={isSubmitting || !mark || overCeiling}>
            {isSubmitting ? 'Saving…' : 'Save mark'}
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
        {assessment && (
          <p className="form-hint">
            Awarded against {assessment.name}
            {maxMarks !== null ? `, which is marked out of ${maxMarks}.` : '.'} This mark cannot be
            moved to a different student or assessment.
          </p>
        )}

        <InputField
          label="Marks obtained"
          type="number"
          min="0.01"
          step="0.01"
          {...register('marksObtained')}
          error={overCeiling ? `Cannot exceed the maximum of ${maxMarks}` : errors.marksObtained?.message}
        />

        <p className="form-hint">
          The original entry time is kept, so this correction does not overwrite when the mark was
          first recorded.
        </p>

        <button className="sr-only" type="submit">
          Save mark
        </button>
      </form>
    </Dialog>
  )
}