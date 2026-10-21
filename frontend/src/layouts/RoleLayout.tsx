import { Bell, Contrast, LogOut, Moon, Search, Sun } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { Brand } from '../components/navigation/Brand'
import { RoleNavigation } from '../components/navigation/RoleNavigation'
import { RoleIdentity } from '../components/navigation/RoleIdentity'
import { FieldCanvas } from '../components/charts/FieldCanvas'
import { LegalLinks } from '../components/legal/LegalLinks'
import type { Role } from '../types/api'

const roleLabel: Record<Role, string> = {
  ADMIN: 'Administration',
  FACULTY: 'Faculty workspace',
  STUDENT: 'Student portal',
}

export function RoleLayout({ role }: { role: Role }) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [theme, setTheme] = useState<'light' | 'dark'>(() => document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light')
  const [designMode, setDesignMode] = useState<'modern' | 'mono'>(() => document.documentElement.dataset.ds === 'mono' ? 'mono' : 'modern')
  const [navOpen, setNavOpen] = useState(false)
  const themePinned = useRef(false)

  useEffect(() => {
    const preference = window.matchMedia('(prefers-color-scheme: dark)')
    const followSystem = () => {
      if (themePinned.current) return
      setTheme(preference.matches ? 'dark' : 'light')
    }
    preference.addEventListener('change', followSystem)
    return () => preference.removeEventListener('change', followSystem)
  }, [])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.dataset.ds = designMode
  }, [theme, designMode])

  // Close the mobile drawer on navigation so a tap always reveals the page.
  // Adjusting state during render is React's documented pattern for reacting to
  // a changed input, and avoids a cascading render from an effect.
  const [lastPath, setLastPath] = useState(location.pathname)
  if (lastPath !== location.pathname) {
    setLastPath(location.pathname)
    setNavOpen(false)
  }

  const toggleTheme = () => {
    themePinned.current = true
    setTheme((current) => current === 'dark' ? 'light' : 'dark')
  }

  const themeLabel = theme === 'dark' ? 'Light mode' : 'Dark mode'
  const designLabel = designMode === 'mono' ? 'Modern' : 'Monochrome'
  const showField = /\/dashboard$/.test(location.pathname)

  const signOut = () => {
    logout()
  }

  return (
    <div className="workspace">
      <a className="skip-link" href="#main-content">Skip to main content</a>

      <aside
        className={`sidebar${navOpen ? ' is-open' : ''}`}
        aria-label={`${roleLabel[role]} navigation`}
        id="workspace-sidebar"
      >
        <div className="sidebar__brand">
          <Brand />
          <div className="sidebar__mobile-actions">
            <button
              type="button"
              className="sidebar__icon-button"
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              onClick={toggleTheme}
            >
              {theme === 'dark' ? <Sun size={18} aria-hidden="true" /> : <Moon size={18} aria-hidden="true" />}
            </button>
            <button
              type="button"
              className="sidebar__icon-button"
              aria-label={`Switch to ${designLabel} design`}
              title={`Switch to ${designLabel} design`}
              onClick={() => setDesignMode((current) => current === 'mono' ? 'modern' : 'mono')}
            >
              <Contrast size={18} aria-hidden="true" />
            </button>
            <button type="button" className="sidebar__icon-button" aria-label="Sign out" title="Sign out" onClick={signOut}>
              <LogOut size={18} aria-hidden="true" />
            </button>
          </div>
        </div>

        <div className="sidebar__role">
          <RoleIdentity role={role} />
        </div>

        <RoleNavigation role={role} onNavigate={() => setNavOpen(false)} />

        <div className="sidebar__footnote">
          <div className="sidebar__account">
            <span className="avatar" aria-hidden="true">{user?.username.slice(0, 1).toUpperCase() ?? 'U'}</span>
            <span className="sidebar__account-copy">
              <strong>{user?.username}</strong>
              <small>{roleLabel[role]}</small>
            </span>
          </div>
          <div className="sidebar__actions">
            <button type="button" className="sidebar__link" onClick={toggleTheme}>
              {themeLabel}
            </button>
            <button type="button" className="sidebar__link" onClick={() => setDesignMode((current) => current === 'mono' ? 'modern' : 'mono')}>
              {designLabel}
            </button>
            <button type="button" className="sidebar__link" onClick={signOut}>Sign out</button>
          </div>
          <nav className="sidebar__legal" aria-label="Legal documents">
            <LegalLinks variant="sidebar" />
          </nav>
        </div>
      </aside>

      <div className="workspace__main">
        <header className="top-bar">
          <button
            type="button"
            className="top-bar__nav-toggle"
            aria-expanded={navOpen}
            aria-controls="workspace-sidebar"
            aria-label={navOpen ? 'Close navigation' : 'Open navigation'}
            onClick={() => setNavOpen((open) => !open)}
          >
            <span className="top-bar__nav-glyph" aria-hidden="true" />
          </button>

          <h1 className="top-bar__title">{roleLabel[role]}</h1>

          <div className="top-bar__spacer" />

          {/*
            Integration points for the V2 shell. Global search and notifications
            are separate phases, so these are presentational and inert rather
            than wired to invented endpoints.
          */}
          <div className="top-bar__search" role="search" aria-label="Global search (coming soon)">
            <Search size={16} aria-hidden="true" />
            <input
              type="search"
              className="top-bar__search-input"
              placeholder="Search"
              aria-label="Global search, not available yet"
              disabled
              readOnly
            />
          </div>
          <button
            type="button"
            className="top-bar__icon-button"
            aria-label="Notifications, not available yet"
            title="Notifications are not available yet"
            disabled
          >
            <Bell size={17} aria-hidden="true" />
          </button>
        </header>

        {navOpen && (
          <button
            type="button"
            className="sidebar__scrim"
            aria-label="Close navigation"
            onClick={() => setNavOpen(false)}
          />
        )}

        {showField && <FieldCanvas />}
        <main className="workspace__content" id="main-content" tabIndex={-1}>
          <div className="route-page" key={location.pathname}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
