interface RoleSwitcherProps {
  activeRole: string
  onUnavailable: (role: string) => void
}

/**
 * Role selector mirroring the preview. CampusCore scopes every workspace to the
 * signed-in account, so roles that the session cannot access are shown but not
 * selectable — route protection is never bypassed from the shell.
 */
export function RoleSwitcher({ activeRole, onUnavailable }: RoleSwitcherProps) {
  const roles = ['ADMIN', 'FACULTY', 'STUDENT']

  return (
    <div className="role-switcher" role="group" aria-label="Workspace role">
      {roles.map((role) => {
        const active = role === activeRole
        return (
          <button
            key={role}
            type="button"
            className="role-switcher__option"
            aria-pressed={active}
            disabled={!active}
            title={active ? undefined : `Sign in with a ${role.toLowerCase()} account to open that workspace`}
            onClick={() => { if (!active) onUnavailable(role) }}
          >
            {role.charAt(0) + role.slice(1).toLowerCase()}
          </button>
        )
      })}
    </div>
  )
}