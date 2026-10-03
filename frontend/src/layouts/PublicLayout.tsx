import { Link, Outlet } from 'react-router-dom'
import { Brand } from '../components/navigation/Brand'
import { LegalLinks } from '../components/legal/LegalLinks'

/**
 * Public shell for pages that must be readable without signing in. Sits
 * outside the authentication guard so it cannot affect role-based routing.
 */
export function PublicLayout() {
  return (
    <div className="legal-page">
      <header className="legal-bar">
        <div className="legal-bar__inner">
          <Brand onCanvas />
          <nav className="legal-bar__nav" aria-label="Legal documents">
            <LegalLinks variant="bar" />
          </nav>
          <Link className="legal-bar__signin" to="/login">Sign in</Link>
        </div>
      </header>

      <main className="legal-page__main" id="main-content" tabIndex={-1}>
        <Outlet />
      </main>

      <footer className="legal-footer">
        <div className="legal-footer__inner">
          <p className="legal-footer__note">
            CampusCore · Student Academic Management System · Academic and project platform
          </p>
          <nav className="legal-footer__legal" aria-label="Legal documents">
            <LegalLinks variant="footer" />
          </nav>
        </div>
      </footer>
    </div>
  )
}