import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { AssessmentResponse } from '../../types/api'
import type { AssessmentUpdateRequest } from './assessments.api'
import { SelectField } from '../../components/forms/SelectField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { InputField } from '../../components/ui/InputField'

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100),
  assessmentType: z.enum(['QUIZ', 'ASSIGNMENT', 'MIDTERM', 'FINAL', 'PROJECT']),
  maxMarks: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a positive value with up to two decimals').refine((value) => Number(value) > 0, 'Maximum marks must be greater than zero'),
  weight: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a percentage with up to two decimals').refine((value) => Number(value) >= 0 && Number(value) <= 100, 'Weight must be between 0 and 100'),
})
type Values = z.infer<typeof schema>

/**
 * Edit an assessment's details.
 *
 * The course is not editable here: an assessment belongs to its offering, and
 * marks recorded against it would be stranded by a move. Maximum marks can be
 * corrected only while nothing has been graded against the assessment, which the
 * server enforces and the notice below explains.
 */
export function AssessmentEditDialog({
  open,
  onOpenChange,
  assessment,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  assessment: AssessmentResponse | null
  onSubmit: (assessmentId: number, request: AssessmentUpdateRequest) => Promise<void>
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', assessmentType: 'QUIZ', maxMarks: '', weight: '' },
  })

  useEffect(
    () =>
      reset({
        name: assessment?.name ?? '',
        assessmentType: assessment?.assessmentType ?? 'QUIZ',
        maxMarks: assessment ? String(assessment.maxMarks) : '',
        weight: assessment ? String(assessment.weight) : '',
      }),
    [assessment, open, reset],
  )

  const submit = handleSubmit(async (values) => {
    if (!assessment) return
    await onSubmit(assessment.assessmentId, {
      name: values.name.trim(),
      assessmentType: values.assessmentType,
      maxMarks: Number(values.maxMarks),
      weight: Number(values.weight),
      assessmentDate: assessment.assessmentDate,
    })
  })

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={assessment ? `Edit ${assessment.name}` : 'Edit assessment'}
      description="Update how this assessment is described and weighted for its course."
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={() => void submit()} disabled={isSubmitting || !assessment}>
            {isSubmitting ? 'Saving…' : 'Save assessment'}
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
        <InputField label="Assessment name" {...register('name')} error={errors.name?.message} />

        <div className="admin-form--two-column">
          <SelectField
            label="Type"
            {...register('assessmentType')}
            error={errors.assessmentType?.message}
          >
            <option value="QUIZ">Quiz</option>
            <option value="ASSIGNMENT">Assignment</option>
            <option value="MIDTERM">Midterm</option>
            <option value="FINAL">Final</option>
            <option value="PROJECT">Project</option>
          </SelectField>

          <InputField
            label="Weight (%)"
            type="number"
            min="0"
            max="100"
            step="0.01"
            {...register('weight')}
            error={errors.weight?.message}
          />
        </div>

        <InputField
          label="Maximum marks"
          type="number"
          min="0.01"
          step="0.01"
          {...register('maxMarks')}
          error={errors.maxMarks?.message}
        />

        <p className="form-hint">
          Once marks have been recorded against this assessment, its maximum marks can no longer be
          changed. Adjust the weight instead.
        </p>

        <button className="sr-only" type="submit">
          Save assessment
        </button>
      </form>
    </Dialog>
  )
}