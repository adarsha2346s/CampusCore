import { Component } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle } from 'lucide-react'
import { Button } from '../components/ui/Button'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
}

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch() {
    // Intentionally do not log errors that could contain user data or credentials.
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="fatal-state" role="alert">
          <div className="fatal-state__icon"><AlertTriangle size={22} aria-hidden="true" /></div>
          <p className="eyebrow">Something went wrong</p>
          <h1>We couldn’t load this page</h1>
          <p className="muted">Try returning to the start of your workspace.</p>
          <Button asChild><Link to="/">Return to workspace</Link></Button>
        </main>
      )
    }
    return this.props.children
  }
}
