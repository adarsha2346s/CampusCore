import { Link } from 'react-router-dom'

export function Brand({ onCanvas = false, label = 'CampusCore home' }: { onCanvas?: boolean; label?: string }) {
  return (
    <Link className={`brand${onCanvas ? ' brand--on-canvas' : ''}`} to="/" aria-label={label}>
      <span className="brand__mark" aria-hidden="true">C</span>
      <span className="brand__name">CampusCore</span>
    </Link>
  )
}
