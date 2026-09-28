import { Navigate } from 'react-router-dom'

export function AttendanceRootRedirect() {
  return <Navigate to="/admin/attendance/sessions" replace />
}
