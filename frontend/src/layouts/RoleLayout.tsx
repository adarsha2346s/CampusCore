import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { LogOut, Menu, X, ChevronDown } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { Brand } from '../components/navigation/Brand'
import { RoleNavigation } from '../components/navigation/RoleNavigation'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import type { Role } from '../types/api'

const roleLabel: Record<Role, string> = {
  ADMIN: 'Administration',
  FACULTY: 'Faculty workspace',
  STUDENT: 'Student portal',
}

export function RoleLayout({ role }: { role: Role }) {
  const { user, logout } = useAuth()
  const [mobileOpenPath, setMobileOpenPath] = useState<string | null>(null)
  const menuButtonRef = useRef<HTMLButtonElement>(null)
  const sidebarRef = useRef<HTMLElement>(null)
  const location = useLocation()
  const mobileOpen = mobileOpenPath === location.pathname
  const pageName = location.pathname.split('/').filter(Boolean).at(-1) ?? 'dashboard'
  const title = pageName === 'dashboard' ? 'Overview' : pageName.replaceAll('-', ' ')

  useEffect(() => {
    if (!mobileOpen) return
    sidebarRef.current?.querySelector<HTMLElement>('a')?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMobileOpenPath(null)
        menuButtonRef.current?.focus()
      }
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [mobileOpen])

  return (
    <div className="workspace">
      <aside
        ref={sidebarRef}
        id="role-navigation-drawer"
        className={`sidebar${mobileOpen ? ' sidebar--open' : ''}`}
        aria-label={`${roleLabel[role]} navigation`}
        onKeyDown={(event) => {
          if (!mobileOpen || event.key !== 'Tab') return
          const items = sidebarRef.current?.querySelectorAll<HTMLElement>('a[href], button:not(:disabled)')
          if (!items?.length) return
          const first = items[0]
          const last = items[items.length - 1]
          if (event.shiftKey && document.activeElement === first) {
            event.preventDefault()
            last.focus()
          } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault()
            first.focus()
          }
        }}
      >
        <div className="sidebar__brand"><Brand /></div>
        <RoleNavigation role={role} onNavigate={() => setMobileOpenPath(null)} />
        <div className="sidebar__footnote">
          <span className="status-dot" aria-hidden="true" />
          <span>CampusCore workspace</span>
        </div>
      </aside>

      {mobileOpen && <button type="button" className="sidebar-scrim" aria-hidden="true" tabIndex={-1} onClick={() => { setMobileOpenPath(null); menuButtonRef.current?.focus() }} />}

      <div className="workspace__main" inert={mobileOpen}>
        <header className="topbar">
          <Button
            variant="ghost"
            size="sm"
            className="mobile-menu-button"
            ref={menuButtonRef}
            aria-controls="role-navigation-drawer"
            aria-label={mobileOpen ? 'Close navigation' : 'Open navigation'}
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpenPath(mobileOpen ? null : location.pathname)}
          >
            {mobileOpen ? <X size={20} /> : <Menu size={20} />}
          </Button>
          <div className="topbar__context">
            <span className="topbar__workspace">{roleLabel[role]}</span>
            <span className="topbar__divider">/</span>
            <span className="topbar__page">{title}</span>
          </div>
          <div className="topbar__account">
            <Badge>{role}</Badge>
            <DropdownMenu.Root>
              <DropdownMenu.Trigger className="account-trigger" aria-label="Account menu">
                <span className="avatar" aria-hidden="true">{user?.username.slice(0, 1).toUpperCase() ?? 'U'}</span>
                <span className="account-trigger__name">{user?.username}</span>
                <ChevronDown size={15} aria-hidden="true" />
              </DropdownMenu.Trigger>
              <DropdownMenu.Portal>
                <DropdownMenu.Content className="dropdown-content" align="end" sideOffset={8}>
                  <div className="dropdown-content__user">
                    <strong>{user?.username}</strong>
                    <span>{user?.email}</span>
                  </div>
                  <DropdownMenu.Separator className="dropdown-separator" />
                  <DropdownMenu.Item className="dropdown-item" onSelect={logout}>
                    <LogOut size={16} aria-hidden="true" /> Sign out
                  </DropdownMenu.Item>
                </DropdownMenu.Content>
              </DropdownMenu.Portal>
            </DropdownMenu.Root>
          </div>
        </header>
        <main className="workspace__content" id="main-content" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
