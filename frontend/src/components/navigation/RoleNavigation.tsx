import {
  Activity, BookOpen, Building2, ClipboardCheck, FileClock, GraduationCap,
  LayoutDashboard, LibraryBig, ListChecks, NotebookTabs, Users, UserRound,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import type { Role } from '../../types/api'

interface NavItem {
  label: string
  path: string
  icon: LucideIcon
}

interface NavGroup {
  label: string
  items: NavItem[]
}

const navigation: Record<Role, NavGroup[]> = {
  ADMIN: [
    { label: 'Workspace', items: [
      { label: 'Overview', path: '/admin/dashboard', icon: LayoutDashboard },
    ] },
    { label: 'Directory', items: [
      { label: 'Users', path: '/admin/users', icon: Users },
      { label: 'Students', path: '/admin/students', icon: GraduationCap },
      { label: 'Faculty', path: '/admin/faculty', icon: UserRound },
      { label: 'Departments', path: '/admin/departments', icon: Building2 },
      { label: 'Courses', path: '/admin/courses', icon: LibraryBig },
    ] },
    { label: 'Academic operations', items: [
      { label: 'Enrollments', path: '/admin/enrollments', icon: NotebookTabs },
      { label: 'Assessments', path: '/admin/assessments', icon: ClipboardCheck },
      { label: 'Marks', path: '/admin/marks', icon: ListChecks },
      { label: 'Attendance', path: '/admin/attendance', icon: Activity },
      { label: 'Grading', path: '/admin/grading', icon: BookOpen },
    ] },
    { label: 'Oversight', items: [
      { label: 'Audit log', path: '/admin/audit', icon: FileClock },
    ] },
  ],
  FACULTY: [{ label: 'Workspace', items: [
    { label: 'Overview', path: '/faculty/dashboard', icon: LayoutDashboard },
    { label: 'Students', path: '/faculty/students', icon: GraduationCap },
    { label: 'Departments', path: '/faculty/departments', icon: Building2 },
    { label: 'Course catalog', path: '/faculty/courses', icon: LibraryBig },
  ] }],
  STUDENT: [{ label: 'Workspace', items: [
    { label: 'Overview', path: '/student/dashboard', icon: LayoutDashboard },
    { label: 'My profile', path: '/student/profile', icon: UserRound },
    { label: 'Course catalog', path: '/student/courses', icon: LibraryBig },
    { label: 'GPA', path: '/student/gpa', icon: BookOpen },
  ] }],
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
    <nav className="side-nav" ref={navRef} aria-label="Main navigation">
      <span className="side-nav__indicator" ref={indicatorRef} aria-hidden="true" />
      {navigation[role].map(({ label, items }) => (
        <div className="side-nav__group" key={label}>
          <p className="side-nav__label">{label}</p>
          {items.map(({ label: itemLabel, path, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              viewTransition
              onClick={onNavigate}
              className={({ isActive }) => `side-nav__link${isActive ? ' is-active' : ''}`}
              end={path !== '/admin/attendance'}
            >
              <Icon size={17} strokeWidth={1.9} aria-hidden="true" />
              <span>{itemLabel}</span>
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  )
}