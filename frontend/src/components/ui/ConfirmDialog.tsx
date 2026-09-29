import { useState } from 'react'
import { ApiError } from '../../lib/api/api-error'
import { Button } from './Button'
import { Dialog } from './Dialog'

const fallbackError = 'The change could not be completed. Please try again.'
const sensitiveOrInternalDetail = /\b(?:password|passwd|credential|authorization|bearer|token|jwt|secret|api[\s_-]?key|private\s+key|exception|stack\s+trace|traceback)\b|-----BEGIN|\beyJ[A-Za-z0-9_-]{8,}\.|\b(?:SQLSTATE|SQLException|SQL syntax|jdbc:|hibernate|org\.springframework|com\.campuscore|java\.)|(?:^|\n)\s*at\s+[\w.$]+/i

function getSafeErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) return fallbackError
  const message = error.message.trim()
  if (!message || message.length > 240 || sensitiveOrInternalDetail.test(message)) return fallbackError
  return message
}

interface ConfirmDialogProps {
  title: string
  description: string
  confirmLabel: string
  onConfirm: () => Promise<void>
  trigger: React.ReactNode
  destructive?: boolean
}

export function ConfirmDialog({ title, description, confirmLabel, onConfirm, trigger, destructive = true }: ConfirmDialogProps) {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function confirm() {
    setBusy(true)
    setError(null)
    try {
      await onConfirm()
      setOpen(false)
    } catch (caughtError) {
      setError(getSafeErrorMessage(caughtError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <span onClick={() => setOpen(true)}>{trigger}</span>
      <Dialog open={open} onOpenChange={(next) => { if (!busy) setOpen(next) }} title={title} description={description} footer={(
        <>
          <Button variant="secondary" onClick={() => setOpen(false)} disabled={busy}>Cancel</Button>
          <Button variant={destructive ? 'danger' : 'primary'} onClick={() => void confirm()} disabled={busy}>
            {busy ? 'Working…' : confirmLabel}
          </Button>
        </>
      )}>
        {error && <p className="form-alert" role="alert">{error}</p>}
      </Dialog>
    </>
  )
}
