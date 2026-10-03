import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/reset.css'
import './styles/tokens.css'
import './styles/base.css'
import './styles/motion.css'
import './styles/shell.css'
import './styles/components.css'
import './styles/dashboard.css'
import './styles/pages.css'
import './styles/legal.css'
import './index.css'
import App from './App.tsx'
import { AppProviders } from './app/providers'
import { AppErrorBoundary } from './app/error-boundary'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppErrorBoundary>
      <AppProviders>
        <App />
      </AppProviders>
    </AppErrorBoundary>
  </StrictMode>,
)
