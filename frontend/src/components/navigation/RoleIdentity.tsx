import { ShieldCheck, GraduationCap, UserRound } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Role } from '../../types/api'

const roleMeta: Record<Role, { label: string; icon: LucideIcon; description: string }> = {
  ADMIN: {
    label: 'Administrator',
    icon: ShieldCheck,
    description: 'Full access to academic records, people and system settings',
  },
  FACULTY: {
    label: 'Faculty',
    icon: GraduationCap,
    description: 'Teaching scope: only courses assigned to you',
  },
  STUDENT: {
    label: 'Student',
    icon: UserRound,
    description: 'Your own academic records',
  },
}

/**
 * Non-interactive identity for the signed-in account.
 *
 * The role comes from the verified server session and is displayed, never
 * selectable: a role is an attribute of the account, so the shell offers no
 * control that could imply it can be changed from the browser. Authorization is
 * enforced by the API regardless of what this component shows.
 */
export function RoleIdentity({ role }: { role: Role }) {
  const { label, icon: Icon, description } = roleMeta[role]

  return (
    <div className="role-identity">
      <p className="sidebar__role-label" id="role-identity-label">Signed in as</p>
      <p
        className="role-identity__value"
        role="status"
        aria-live="polite"
        aria-labelledby="role-identity-label"
        title={description}
      >
        <Icon size={15} strokeWidth={2} aria-hidden="true" />
        <span>{label}</span>
      </p>
    </div>
  )
}
