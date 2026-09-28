import { AlertCircle, RotateCw } from 'lucide-react'
import { ApiError } from '../../lib/api/api-error'
import { Button } from '../ui/Button'

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message = error instanceof ApiError
    ? error.message
    : 'We could not complete that request. Please try again.'
  return (
    <div className="state-panel state-panel--error" role="alert">
      <span className="state-panel__icon"><AlertCircle size={22} aria-hidden="true" /></span>
      <h2>We couldn’t load this information</h2>
      <p>{message}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          <RotateCw size={16} aria-hidden="true" /> Try again
        </Button>
      )}
    </div>
  )
}
