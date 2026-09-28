/* eslint-disable react-refresh/only-export-components */
import { lazy } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth, RequireRole } from './route-guards'
import { roleHome } from '../lib/role-home'
import { ForbiddenPage, NotFoundPage } from './SystemPages'
import { AuthLayout } from '../layouts/AuthLayout'
import { useAuth } from '../features/auth/auth-context'

const LoginPage = lazy(() => import('../features/auth/LoginPage').then((module) => ({ default: module.LoginPage })))
const AdminLayout = lazy(() => import('../layouts/AdminLayout').then((module) => ({ default: module.AdminLayout })))
const FacultyLayout = lazy(() => import('../layouts/FacultyLayout').then((module) => ({ default: module.FacultyLayout })))
const StudentLayout = lazy(() => import('../layouts/StudentLayout').then((module) => ({ default: module.StudentLayout })))
const AdminDashboardPage = lazy(() => import('../features/admin-dashboard/AdminDashboardPage').then((module) => ({ default: module.AdminDashboardPage })))
const UsersPage = lazy(() => import('../features/users/UsersPage').then((module) => ({ default: module.UsersPage })))
const StudentsPage = lazy(() => import('../features/students/StudentsPage').then((module) => ({ default: module.StudentsPage })))
const FacultyPage = lazy(() => import('../features/faculty/FacultyPage').then((module) => ({ default: module.FacultyPage })))
const DepartmentsPage = lazy(() => import('../features/departments/DepartmentsPage').then((module) => ({ default: module.DepartmentsPage })))
const CoursesPage = lazy(() => import('../features/courses/CoursesPage').then((module) => ({ default: module.CoursesPage })))
const EnrollmentsPage = lazy(() => import('../features/enrollments/EnrollmentsPage').then((module) => ({ default: module.EnrollmentsPage })))
const AssessmentsPage = lazy(() => import('../features/assessments/AssessmentsPage').then((module) => ({ default: module.AssessmentsPage })))
const MarksPage = lazy(() => import('../features/marks/MarksPage').then((module) => ({ default: module.MarksPage })))
const AttendanceSessionsPage = lazy(() => import('../features/attendance/AttendanceSessionsPage').then((module) => ({ default: module.AttendanceSessionsPage })))
const AttendanceRecordsPage = lazy(() => import('../features/attendance/AttendanceRecordsPage').then((module) => ({ default: module.AttendanceRecordsPage })))
const AttendanceRootRedirect = lazy(() => import('../features/attendance/AttendanceRoutes').then((module) => ({ default: module.AttendanceRootRedirect })))
const GradingPage = lazy(() => import('../features/grading/GradingPage').then((module) => ({ default: module.GradingPage })))
const AuditLogPage = lazy(() => import('../features/audit/AuditLogPage').then((module) => ({ default: module.AuditLogPage })))
const FacultyOverviewPage = lazy(() => import('../features/faculty-workspace/FacultyOverviewPage').then((module) => ({ default: module.FacultyOverviewPage })))
const FacultyStudentsPage = lazy(() => import('../features/faculty-workspace/FacultyStudentsPage').then((module) => ({ default: module.FacultyStudentsPage })))
const FacultyCatalogPage = lazy(() => import('../features/faculty-workspace/FacultyCatalogPage').then((module) => ({ default: module.FacultyCatalogPage })))
const StudentOverviewPage = lazy(() => import('../features/student-workspace/StudentOverviewPage').then((module) => ({ default: module.StudentOverviewPage })))
const StudentRecordPage = lazy(() => import('../features/student-workspace/StudentRecordPage').then((module) => ({ default: module.StudentRecordPage })))
const StudentCatalogPage = lazy(() => import('../features/student-workspace/StudentCatalogPage').then((module) => ({ default: module.StudentCatalogPage })))

function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? roleHome(user.role) : '/login'} replace />
}

export const router = createBrowserRouter([
  {
    element: <AuthLayout />,
    children: [{ path: '/login', element: <LoginPage /> }],
  },
  { path: '/', element: <HomeRedirect /> },
  { path: '/forbidden', element: <ForbiddenPage /> },
  { path: '/not-found', element: <NotFoundPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <RequireRole allowedRoles={['ADMIN']} />,
        children: [{
          path: '/admin',
          element: <AdminLayout />,
          children: [
            { index: true, element: <Navigate to="dashboard" replace /> },
            { path: 'dashboard', element: <AdminDashboardPage /> },
            { path: 'users', element: <UsersPage /> },
            { path: 'students', element: <StudentsPage /> },
            { path: 'faculty', element: <FacultyPage /> },
            { path: 'departments', element: <DepartmentsPage /> },
            { path: 'courses', element: <CoursesPage /> },
            { path: 'enrollments', element: <EnrollmentsPage /> },
            { path: 'assessments', element: <AssessmentsPage /> },
            { path: 'marks', element: <MarksPage /> },
            { path: 'attendance', element: <AttendanceRootRedirect /> },
            { path: 'attendance/sessions', element: <AttendanceSessionsPage /> },
            { path: 'attendance/records', element: <AttendanceRecordsPage /> },
            { path: 'grading', element: <GradingPage /> },
            { path: 'audit', element: <AuditLogPage /> },
            { path: '*', element: <NotFoundPage /> },
          ],
        }],
      },
      {
        element: <RequireRole allowedRoles={['FACULTY']} />,
        children: [{
          path: '/faculty',
          element: <FacultyLayout />,
          children: [
            { index: true, element: <Navigate to="dashboard" replace /> },
            { path: 'dashboard', element: <FacultyOverviewPage /> },
            { path: 'students', element: <FacultyStudentsPage /> },
            { path: 'departments', element: <FacultyCatalogPage kind="departments" /> },
            { path: 'courses', element: <FacultyCatalogPage kind="courses" /> },
            { path: '*', element: <NotFoundPage /> },
          ],
        }],
      },
      {
        element: <RequireRole allowedRoles={['STUDENT']} />,
        children: [{
          path: '/student',
          element: <StudentLayout />,
          children: [
            { index: true, element: <Navigate to="dashboard" replace /> },
            { path: 'dashboard', element: <StudentOverviewPage /> },
            { path: 'profile', element: <StudentRecordPage section="profile" /> },
            { path: 'courses', element: <StudentCatalogPage /> },
            { path: 'gpa', element: <StudentRecordPage section="gpa" /> },
            { path: '*', element: <NotFoundPage /> },
          ],
        }],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
