import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRight, Eye, EyeOff, LockKeyhole, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from './auth-context'
import { ApiError } from '../../lib/api/api-error'
import { roleHome } from '../../lib/role-home'
import { Button } from '../../components/ui/Button'
import { InputField } from '../../components/ui/InputField'

const loginSchema = z.object({
  username: z.string().trim().min(1, 'Enter your username.'),
  password: z.string().min(1, 'Enter your password.'),
})

type LoginValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '' },
  })

  if (isAuthenticated) return <Navigate to="/" replace />

  const submit = handleSubmit(async (credentials) => {
    setServerError(null)
    try {
      const user = await login(credentials)
      navigate(roleHome(user.role), { replace: true })
    } catch (error) {
      const message = error instanceof ApiError
        ? error.message
        : error instanceof Error
          ? error.message
          : 'Sign in could not be completed. Please try again.'
      setServerError(message)
    }
  })

  return (
    <div className="login-card">
      <div className="login-card__heading">
        <h1>Sign in to your workspace</h1>
        <p>Use the account provided by your campus administrator.</p>
      </div>

      <form className="login-form" onSubmit={submit} noValidate>
        {serverError && <div className="form-alert" role="alert">{serverError}</div>}
        <div className="field-with-icon">
          <UserRound size={17} aria-hidden="true" />
          <InputField
            label="Username"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            placeholder="Enter your username"
            autoFocus
            error={errors.username?.message}
            {...register('username')}
          />
        </div>
        <div className="field-with-icon field-with-icon--password">
          <LockKeyhole size={17} aria-hidden="true" />
          <InputField
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Enter your password"
            error={errors.password?.message}
            {...register('password')}
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowPassword((shown) => !shown)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            aria-pressed={showPassword}
          >
            {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
          </button>
        </div>
        <Button className="login-submit" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Signing in…' : 'Sign in'}
          {!isSubmitting && <ArrowRight size={17} aria-hidden="true" />}
        </Button>
      </form>
      <div className="login-card__note">
        <span>Need access?</span> Contact your campus administrator.
      </div>
      <Link className="sr-only" to="/forbidden">Access information</Link>
    </div>
  )
}
