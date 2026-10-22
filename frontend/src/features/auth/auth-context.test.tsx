import { act, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './auth-context'
import { handleForbidden, handleUnauthorized } from './session'
import { queryClient } from '../../lib/api/query-client'
import { getCurrentUser, loginRequest } from './auth.api'
import type { MeResponse } from '../../types/api'

vi.mock('./auth.api', () => ({
  getCurrentUser: vi.fn(),
  loginRequest: vi.fn(),
}))

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedLogin = vi.mocked(loginRequest)

const admin: MeResponse = {
  userId: 1,
  username: 'admin-account',
  email: 'admin@example.test',
  role: 'ADMIN',
  active: true,
}

const faculty: MeResponse = {
  userId: 2,
  username: 'faculty-account',
  email: 'faculty@example.test',
  role: 'FACULTY',
  active: true,
}

/**
 * Signs the shell in and reports the identity it holds.
 *
 * The forbidden handler compares the role it believes the account has with the
 * role the server reports, so the account has to be signed in before the
 * behaviour under test can be observed.
 */
async function signInAs(identity: MeResponse) {
  mockedLogin.mockResolvedValue({ token: 'token', username: identity.username, role: identity.role })
  mockedGetCurrentUser.mockResolvedValue(identity)

  let login: ((credentials: { username: string; password: string }) => Promise<MeResponse>) | undefined

  function Probe() {
    const auth = useAuth()
    login = auth.login
    return <span>{auth.user?.role ?? 'signed out'}</span>
  }

  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  )

  await act(async () => {
    await login?.({ username: identity.username, password: 'secret' })
  })

  await screen.findByText(identity.role)
}

/**
 * The 403 handling.
 *
 * A 403 is the ordinary answer to a request for a record outside the caller's
 * scope, so it must not throw away the rest of the workspace's cached data.
 * Clearing the cache on every such refusal emptied every other workspace's
 * data and refetched it, which turned one refused record into a storm of
 * requests. Only a change of role, which can genuinely take privileges away,
 * invalidates anything, and then only the administrator-scoped entries.
 */
describe('forbidden handling', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    queryClient.clear()
  })

  it('leaves unrelated cached data alone when the role has not changed', async () => {
    await signInAs(admin)

    queryClient.setQueryData(['faculty', 'courses', 1], { courseId: 1 })
    queryClient.setQueryData(['admin', 'courses'], [{ courseId: 7 }])

    await act(async () => {
      handleForbidden()
    })

    await waitFor(() => expect(mockedGetCurrentUser).toHaveBeenCalled())

    expect(queryClient.getQueryData(['faculty', 'courses', 1])).toEqual({ courseId: 1 })
    expect(queryClient.getQueryData(['admin', 'courses'])).toEqual([{ courseId: 7 }])
  })

  it('invalidates the administrator-scoped entries when the role has changed', async () => {
    await signInAs(admin)

    queryClient.setQueryData(['admin', 'courses'], [{ courseId: 7 }])
    queryClient.setQueryData(['faculty', 'courses', 1], { courseId: 1 })

    mockedGetCurrentUser.mockResolvedValue(faculty)

    await act(async () => {
      handleForbidden()
    })

    await waitFor(() => expect(screen.getByText('FACULTY')).toBeInTheDocument())

    expect(queryClient.getQueryState(['admin', 'courses'])?.isInvalidated).toBe(true)
    // The faculty-scoped entry belongs to the account itself and stays valid.
    expect(queryClient.getQueryState(['faculty', 'courses', 1])?.isInvalidated).toBe(false)
    expect(queryClient.getQueryData(['faculty', 'courses', 1])).toEqual({ courseId: 1 })
  })

  it('does not sign the account out when the server merely refuses a record', async () => {
    await signInAs(admin)

    await act(async () => {
      handleForbidden()
    })

    await waitFor(() => expect(mockedGetCurrentUser).toHaveBeenCalled())

    expect(screen.getByText('ADMIN')).toBeInTheDocument()
  })
})

/**
 * The 401 handling is a different matter and is left as it was: an expired or
 * invalid session must drop the account and the data cached for it.
 */
describe('unauthorized handling', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    queryClient.clear()
  })

  it('drops the session and the cached data', async () => {
    await signInAs(admin)

    queryClient.setQueryData(['admin', 'courses'], [{ courseId: 7 }])

    await act(async () => {
      handleUnauthorized()
    })

    await waitFor(() => expect(screen.getByText('signed out')).toBeInTheDocument())
    expect(queryClient.getQueryData(['admin', 'courses'])).toBeUndefined()
  })
})
