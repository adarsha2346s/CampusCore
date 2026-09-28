import { Landmark } from 'lucide-react'
import { Link } from 'react-router-dom'

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link className={`brand${compact ? ' brand--compact' : ''}`} to="/" aria-label="CampusCore home">
      <span className="brand__mark"><Landmark size={19} strokeWidth={2.2} aria-hidden="true" /></span>
      {!compact && <span className="brand__name">Campus<span>Core</span></span>}
    </Link>
  )
}
