import { useState } from 'react'
import { Button } from './Button'
import { Dialog } from './Dialog'

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
    } catch {
      setError('The change could not be completed. Please try again.')
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
