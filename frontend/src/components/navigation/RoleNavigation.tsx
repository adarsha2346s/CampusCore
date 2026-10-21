import {
  Activity, BookOpen, Building2, ClipboardCheck, FileClock, GraduationCap,
  LayoutDashboard, LibraryBig, ListChecks, NotebookTabs, Users, UserRound,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { BorderGlow } from '../ui/BorderGlow'
import { useEffect, useRef } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import type { Role } from '../../types/api'

interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  /** Nested items collapse into the parent link. */
  matchPrefix?: boolean
}

interface NavGroup {
  label: string
  items: NavItem[]
}

/**
 * Navigation is derived from the signed-in account's role only.
 *
 * Every entry points at a route that exists and is guarded by the backend. Items
 * whose pages are not implemented yet are intentionally absent rather than shown
 * as dead links, so nothing here can mislead a user about what is available.
 */
const navigation: Record<Role, NavGroup[]> = {
  ADMIN: [
    { label: 'Dashboard', items: [
      { label: 'Dashboard', path: '/admin/dashboard', icon: LayoutDashboard },
    ] },
    { label: 'People', items: [
      { label: 'Users', path: '/admin/users', icon: Users },
      { label: 'Students', path: '/admin/students', icon: GraduationCap },
      { label: 'Faculty', path: '/admin/faculty', icon: UserRound },
    ] },
    { label: 'Academic', items: [
      { label: 'Departments', path: '/admin/departments', icon: Building2 },
      { label: 'Courses', path: '/admin/courses', icon: LibraryBig },
      { label: 'Enrollments', path: '/admin/enrollments', icon: NotebookTabs },
      { label: 'Assessments', path: '/admin/assessments', icon: ClipboardCheck },
      { label: 'Marks', path: '/admin/marks', icon: ListChecks },
      { label: 'Attendance', path: '/admin/attendance', icon: Activity, matchPrefix: true },
      { label: 'Grading', path: '/admin/grading', icon: BookOpen },
    ] },
    { label: 'System', items: [
      { label: 'Audit Logs', path: '/admin/audit', icon: FileClock },
    ] },
  ],
  FACULTY: [
    { label: 'Dashboard', items: [
      { label: 'Dashboard', path: '/faculty/dashboard', icon: LayoutDashboard },
    ] },
    { label: 'Teaching', items: [
      { label: 'My Courses', path: '/faculty/courses', icon: LibraryBig },
      { label: 'Assessments', path: '/faculty/assessments', icon: ClipboardCheck },
      { label: 'Marks', path: '/faculty/marks', icon: ListChecks },
      { label: 'Attendance', path: '/faculty/attendance', icon: Activity },
      { label: 'My Students', path: '/faculty/students', icon: GraduationCap },
    ] },
    { label: 'Reference', items: [
      { label: 'Course Catalog', path: '/faculty/courses/catalog', icon: BookOpen },
      { label: 'Departments', path: '/faculty/departments', icon: Building2 },
    ] },
  ],
  STUDENT: [
    { label: 'Dashboard', items: [
      { label: 'Dashboard', path: '/student/dashboard', icon: LayoutDashboard },
    ] },
    { label: 'Academic', items: [
      { label: 'My Courses', path: '/student/courses', icon: LibraryBig },
      { label: 'My Marks', path: '/student/marks', icon: ListChecks },
      { label: 'My Attendance', path: '/student/attendance', icon: Activity },
      { label: 'GPA', path: '/student/gpa', icon: BookOpen },
    ] },
    { label: 'Profile', items: [
      { label: 'My Profile', path: '/student/profile', icon: UserRound },
    ] },
  ],
}

export function RoleNavigation({ role, onNavigate }: { role: Role; onNavigate?: () => void }) {
  const navRef = useRef<HTMLElement>(null)
  const indicatorRef = useRef<HTMLSpanElement>(null)
  const location = useLocation()

  useEffect(() => {
    const nav = navRef.current
    const indicator = indicatorRef.current
    if (!nav || !indicator) return

    const active = nav.querySelector<HTMLElement>('[aria-current="page"]')
    if (!active) {
      indicator.dataset.visible = 'false'
      return
    }

    const move = () => {
      const navBox = nav.getBoundingClientRect()
      const activeBox = active.getBoundingClientRect()
      indicator.style.width = `${activeBox.width}px`
      indicator.style.height = `${activeBox.height}px`
      indicator.style.transform = `translate(${activeBox.left - navBox.left + nav.scrollLeft}px, ${activeBox.top - navBox.top + nav.scrollTop}px)`
      indicator.dataset.visible = 'true'
    }

    move()
    const observer = new ResizeObserver(move)
    observer.observe(nav)
    window.addEventListener('resize', move)
    document.fonts?.ready.then(move).catch(() => undefined)
    return () => {
      observer.disconnect()
      window.removeEventListener('resize', move)
    }
  }, [location.pathname, role])

  return (
    <nav className="side-nav" ref={navRef} aria-label={`${role} navigation`}>
      <span className="side-nav__indicator" ref={indicatorRef} aria-hidden="true" />
      {navigation[role].map(({ label, items }) => (
        <div className="side-nav__group" key={label}>
          <p className="side-nav__label">{label}</p>
          {items.map(({ label: itemLabel, path, icon: Icon, matchPrefix }) => (
            <NavLink
              key={path}
              to={path}
              viewTransition
              onClick={onNavigate}
              className={({ isActive }) => `side-nav__link${isActive ? ' is-active' : ''}`}
              end={!matchPrefix}
            >
              {({ isActive }) => (
                <>
                  {isActive && <BorderGlow className="side-nav__glow" />}
                  <Icon size={17} strokeWidth={1.9} aria-hidden="true" />
                  <span>{itemLabel}</span>
                </>
              )}
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  )
}
