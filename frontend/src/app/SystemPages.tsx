import { ArrowLeft, Home, ShieldX } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { PageHeader } from '../components/ui/PageHeader'
import { useAuth } from '../features/auth/auth-context'
import { roleHome } from '../lib/role-home'

export function ForbiddenPage() {
  const { user } = useAuth()
  return (
    <main className="system-page">
      <div className="system-page__icon"><ShieldX size={26} aria-hidden="true" /></div>
      <PageHeader eyebrow="Access restricted" title="You don’t have access to this page" description="Your account role does not include this workspace. If you need access, contact your campus administrator." />
      <Button asChild><Link to={user ? roleHome(user.role) : '/login'}><ArrowLeft size={16} /> Return to your workspace</Link></Button>
    </main>
  )
}

export function NotFoundPage() {
  return (
    <main className="system-page">
      <div className="system-page__code">404</div>
      <PageHeader eyebrow="Page not found" title="We can’t find that page" description="The address may be incorrect, or the page may have moved." />
      <Button asChild><Link to="/"><Home size={16} /> Go to workspace</Link></Button>
    </main>
  )
}
