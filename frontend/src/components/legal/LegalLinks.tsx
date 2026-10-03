import { Link } from 'react-router-dom'

/**
 * Shared Privacy Policy / Terms links. Used on the sign-in page, the public
 * legal shell and the authenticated sidebar so the documents are reachable
 * everywhere without duplicating markup.
 */
export function LegalLinks({ variant = 'inline' }: { variant?: 'inline' | 'bar' | 'footer' | 'sidebar' }) {
  return (
    <span className={`legal-links legal-links--${variant}`}>
      <Link to="/privacy">Privacy Policy</Link>
      <span className="legal-links__divider" aria-hidden="true">·</span>
      <Link to="/terms">Terms &amp; Conditions</Link>
    </span>
  )
}