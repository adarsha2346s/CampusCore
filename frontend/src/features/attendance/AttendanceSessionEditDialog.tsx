import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { AttendanceSessionResponse } from '../../types/api'
import type { AttendanceSessionUpdateRequest } from './attendance.api'
import { InputField } from '../../components/ui/InputField'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'

const schema = z.object({
  sessionDate: z.string().min(1, 'Session date is required'),
  topic: z.string().trim().min(1, 'Topic is required').max(255),
})
type Values = z.infer<typeof schema>

/**
 * Edit an attendance session.
 *
 * Course and lecturer are shown as read-only context because attendance records
 * are filed against the session: moving it to another class would file those
 * records under the wrong offering. The date stays editable even once attendance
 * exists, since correcting a mistyped date does not alter what was recorded.
 */
export function AttendanceSessionEditDialog({
  open,
  onOpenChange,
  session,
  context,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  session: AttendanceSessionResponse | null
  context: string
  onSubmit: (sessionId: number, request: AttendanceSessionUpdateRequest) => Promise<void>
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { sessionDate: '', topic: '' },
  })

  useEffect(
    () =>
      reset({
        sessionDate: session ? session.sessionDate.slice(0, 10) : '',
        topic: session?.topic ?? '',
      }),
    [session, open, reset],
  )

  const submit = handleSubmit(async (values) => {
    if (!session) return
    await onSubmit(session.attendanceSessionId, {
      sessionDate: values.sessionDate,
      topic: values.topic.trim(),
    })
  })

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Edit attendance session"
      description={context}
      footer={
        <>
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={() => void submit()} disabled={isSubmitting || !session}>
            {isSubmitting ? 'Saving…' : 'Save session'}
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
        <InputField
          label="Session date"
          type="date"
          {...register('sessionDate')}
          error={errors.sessionDate?.message}
        />

        <InputField label="Topic" {...register('topic')} error={errors.topic?.message} />

        <p className="form-hint">
          The course and lecturer are fixed. Attendance already recorded against this session stays
          attached to it.
        </p>

        <button className="sr-only" type="submit">
          Save session
        </button>
      </form>
    </Dialog>
  )
}