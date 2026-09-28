import { toast } from 'sonner'
import { ApiError } from '../../lib/api/api-error'

export function notifyError(error: unknown, fallback: string) {
  toast.error(error instanceof ApiError ? error.message : fallback)
}
