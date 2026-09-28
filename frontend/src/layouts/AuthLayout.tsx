import { ArrowLeft, Landmark, ShieldCheck } from 'lucide-react'
import { Link, Outlet } from 'react-router-dom'

export function AuthLayout() {
  return (
    <main className="auth-page">
      <section className="auth-brand-panel" aria-label="CampusCore introduction">
        <Link to="/login" className="auth-brand">
          <span className="auth-brand__mark"><Landmark size={22} aria-hidden="true" /></span>
          <span>Campus<span>Core</span></span>
        </Link>
        <div className="auth-brand-panel__message">
          <p className="eyebrow eyebrow--light">A clearer view of campus life</p>
          <h1>Academic operations, thoughtfully connected.</h1>
          <p>One calm, reliable workspace for the people who help a university move forward.</p>
        </div>
        <div className="auth-brand-panel__footer">
          <ShieldCheck size={17} aria-hidden="true" />
          <span>Secure access for your campus community</span>
        </div>
        <div className="auth-decoration auth-decoration--one" aria-hidden="true" />
        <div className="auth-decoration auth-decoration--two" aria-hidden="true" />
      </section>
      <section className="auth-form-panel">
        <Link className="auth-back" to="/login"><ArrowLeft size={16} aria-hidden="true" /> CampusCore sign in</Link>
        <Outlet />
        <p className="auth-legal">CampusCore · Academic management workspace</p>
      </section>
    </main>
  )
}
