import {
  Activity, BookOpen, Building2, ClipboardCheck, FileClock, GraduationCap,
  LayoutDashboard, LibraryBig, ListChecks, NotebookTabs, Users, UserRound,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import type { Role } from '../../types/api'

interface NavItem {
  label: string
  path: string
  icon: LucideIcon
}

const navigation: Record<Role, NavItem[]> = {
  ADMIN: [
    { label: 'Overview', path: '/admin/dashboard', icon: LayoutDashboard },
    { label: 'Users', path: '/admin/users', icon: Users },
    { label: 'Students', path: '/admin/students', icon: GraduationCap },
    { label: 'Faculty', path: '/admin/faculty', icon: UserRound },
    { label: 'Departments', path: '/admin/departments', icon: Building2 },
    { label: 'Courses', path: '/admin/courses', icon: LibraryBig },
    { label: 'Enrollments', path: '/admin/enrollments', icon: NotebookTabs },
    { label: 'Assessments', path: '/admin/assessments', icon: ClipboardCheck },
    { label: 'Marks', path: '/admin/marks', icon: ListChecks },
    { label: 'Attendance', path: '/admin/attendance', icon: Activity },
    { label: 'Grading', path: '/admin/grading', icon: BookOpen },
    { label: 'Audit log', path: '/admin/audit', icon: FileClock },
  ],
  FACULTY: [
    { label: 'Overview', path: '/faculty/dashboard', icon: LayoutDashboard },
    { label: 'Students', path: '/faculty/students', icon: GraduationCap },
    { label: 'Departments', path: '/faculty/departments', icon: Building2 },
    { label: 'Course catalog', path: '/faculty/courses', icon: LibraryBig },
  ],
  STUDENT: [
    { label: 'Overview', path: '/student/dashboard', icon: LayoutDashboard },
    { label: 'My profile', path: '/student/profile', icon: UserRound },
    { label: 'Course catalog', path: '/student/courses', icon: LibraryBig },
    { label: 'GPA', path: '/student/gpa', icon: BookOpen },
  ],
}

export function RoleNavigation({ role, onNavigate }: { role: Role; onNavigate?: () => void }) {
  return (
    <nav className="side-nav" aria-label="Main navigation">
      <p className="side-nav__label">Workspace</p>
      {navigation[role].map(({ label, path, icon: Icon }) => (
        <NavLink
          key={path}
          to={path}
          onClick={onNavigate}
          className={({ isActive }) => `side-nav__link${isActive ? ' is-active' : ''}`}
          end={path !== '/admin/attendance'}
        >
          <Icon size={18} strokeWidth={1.8} aria-hidden="true" />
          <span>{label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
