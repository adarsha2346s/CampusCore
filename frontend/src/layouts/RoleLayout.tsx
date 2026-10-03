import { Contrast, LogOut, Moon, Sun } from 'lucide-react'
import { toast } from 'sonner'
import { useEffect, useRef, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { Brand } from '../components/navigation/Brand'
import { RoleNavigation } from '../components/navigation/RoleNavigation'
import { RoleSwitcher } from '../components/navigation/RoleSwitcher'
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
      <aside className="sidebar" aria-label={`${roleLabel[role]} navigation`}>
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
          <span className="sidebar__role-label" id="role-switcher-label">Viewing as</span>
          <RoleSwitcher
            activeRole={role}
            onUnavailable={(target) => toast.info(`Sign in with a ${target.toLowerCase()} account to open that workspace`)}
          />
        </div>

        <RoleNavigation role={role} />

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