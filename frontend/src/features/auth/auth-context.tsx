/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { PropsWithChildren } from 'react'
import { queryClient } from '../../lib/api/query-client'
import type { LoginRequest, MeResponse, Role } from '../../types/api'
import { getCurrentUser, loginRequest } from './auth.api'
import { clearSession, registerUnauthorizedHandler, setAccessToken } from './session'

interface AuthContextValue {
  user: MeResponse | null
  isAuthenticated: boolean
  login: (credentials: LoginRequest) => Promise<MeResponse>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)
const supportedRoles: Role[] = ['ADMIN', 'FACULTY', 'STUDENT']

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<MeResponse | null>(null)

  useEffect(() => registerUnauthorizedHandler(() => {
    setUser(null)
    queryClient.clear()
  }), [])

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
    queryClient.clear()
  }, [])

  const login = useCallback(async (credentials: LoginRequest) => {
    const result = await loginRequest(credentials)
    if (!supportedRoles.includes(result.role)) {
      throw new Error('This account has an unsupported role.')
    }

    setAccessToken(result.token)
    try {
      const currentUser = await getCurrentUser()
      if (!supportedRoles.includes(currentUser.role) || currentUser.role !== result.role) {
        throw new Error('The account role could not be verified.')
      }
      if (!currentUser.active) throw new Error('This account is inactive.')
      setUser(currentUser)
      return currentUser
    } catch (error) {
      clearSession()
      setUser(null)
      throw error
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    isAuthenticated: user !== null,
    login,
    logout,
  }), [user, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
