import { Badge } from '../../components/ui/Badge'

export function StatusBadge({ status }: { status: string }) {
  return <Badge className={`status-badge status-badge--${status.toLowerCase()}`}><span className="status-badge__dot" aria-hidden="true" />{status.replaceAll('_', ' ')}</Badge>
}
