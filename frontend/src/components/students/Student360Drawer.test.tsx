import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/harness'
import { Student360Drawer } from './Student360Drawer'
import { getStudentEnrollments } from '../../features/students/students.api'
import { getCourses } from '../../features/courses/courses.api'
import { getUser } from '../../features/users/users.api'
import {
  getAttendanceRecords,
  getAttendanceRecordsByEnrollment,
} from '../../features/attendance/attendance.api'
import { getEnrollments } from '../../features/enrollments/enrollments.api'
import { useAuth } from '../../features/auth/auth-context'
import { ApiError } from '../../lib/api/api-error'
import type {
  AttendanceRecordResponse,
  CourseResponse,
  EnrollmentResponse,
  MeResponse,
  StudentResponse,
  UserResponse,
} from '../../types/api'

vi.mock('../../features/auth/auth-context', () => ({ useAuth: vi.fn() }))

vi.mock('../../features/students/students.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../features/students/students.api')>()),
  getStudentEnrollments: vi.fn(),
}))

vi.mock('../../features/courses/courses.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../features/courses/courses.api')>()),
  getCourses: vi.fn(),
}))

vi.mock('../../features/users/users.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../features/users/users.api')>()),
  getUser: vi.fn(),
}))

vi.mock('../../features/attendance/attendance.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../features/attendance/attendance.api')>()),
  getAttendanceRecordsByEnrollment: vi.fn(),
  getAttendanceRecords: vi.fn(),
}))

vi.mock('../../features/enrollments/enrollments.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../features/enrollments/enrollments.api')>()),
  getEnrollments: vi.fn(),
}))

const mockedEnrollments = vi.mocked(getStudentEnrollments)
const mockedCourses = vi.mocked(getCourses)
const mockedUser = vi.mocked(getUser)
const mockedAttendance = vi.mocked(getAttendanceRecordsByEnrollment)
const mockedUseAuth = vi.mocked(useAuth)
const mockedAdminEnrollments = vi.mocked(getEnrollments)
const mockedAdminRecords = vi.mocked(getAttendanceRecords)

const student: StudentResponse = {
  studentId: 51,
  userId: 1051,
  departmentId: 1,
  enrollmentNumber: 'DEMO-2026-001',
  firstName: 'Avery',
  lastName: 'Chen',
  dateOfBirth: '2004-05-06',
  phone: '555-0100',
  admissionYear: 2026,
  status: 'ACTIVE',
}

const course = (overrides: Partial<CourseResponse> = {}): CourseResponse => ({
  courseId: 7,
  departmentId: 1,
  courseCode: 'CSE101',
  courseName: 'Data Structures',
  credits: 4,
  capacity: 40,
  status: 'ACTIVE',
  ...overrides,
})

const enrollment = (overrides: Partial<EnrollmentResponse> = {}): EnrollmentResponse => ({
  enrollmentId: 300,
  studentId: 51,
  courseId: 7,
  semester: 'Fall',
  academicYear: '2026-2027',
  enrollmentDate: '2026-08-10',
  status: 'ENROLLED',
  enrollmentNumber: 'DEMO-2026-001',
  studentFirstName: 'Avery',
  studentLastName: 'Chen',
  ...overrides,
})

const record = (overrides: Partial<AttendanceRecordResponse> = {}): AttendanceRecordResponse => ({
  attendanceRecordId: 900,
  attendanceSessionId: 40,
  enrollmentId: 300,
  status: 'PRESENT',
  ...overrides,
})

const account: UserResponse = {
  userId: 1051,
  username: 'avery.chen',
  email: 'avery.chen@example.test',
  role: 'STUDENT',
  active: true,
}

function signedInAs(role: MeResponse['role']) {
  mockedUseAuth.mockReturnValue({
    user: { userId: 1, username: 'signed-in', email: 'signed-in@example.test', role, active: true },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  })
}

function renderDrawer(client?: QueryClient) {
  return renderWithProviders(
    <Student360Drawer
      student={student}
      onOpenChange={() => {}}
      departmentName="Computer Science and Engineering"
    />,
    client,
  )
}

describe('Student360Drawer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    signedInAs('FACULTY')
    mockedEnrollments.mockResolvedValue([enrollment()])
    mockedCourses.mockResolvedValue([course()])
    mockedUser.mockResolvedValue(account)
    mockedAttendance.mockResolvedValue([record()])
  })

  // ------------------------------------------------------------------
  // Administrator
  // ------------------------------------------------------------------

  it('shows the authorized student details to an administrator', async () => {
    signedInAs('ADMIN')

    renderDrawer()

    expect(await screen.findByText('Avery Chen')).toBeInTheDocument()
    expect(screen.getByText('DEMO-2026-001')).toBeInTheDocument()
    // The date format follows the browser locale, so only the year is pinned.
    expect(await screen.findByText(/2004/)).toBeInTheDocument()
    expect(screen.getByText('555-0100')).toBeInTheDocument()
    expect(await screen.findByText('avery.chen')).toBeInTheDocument()
  })

  it('lets an administrator read the account behind the student', async () => {
    signedInAs('ADMIN')

    renderDrawer()

    await waitFor(() => expect(mockedUser).toHaveBeenCalledWith(1051))
  })

  // ------------------------------------------------------------------
  // Faculty
  // ------------------------------------------------------------------

  it('shows a faculty member the student they teach', async () => {
    renderDrawer()

    expect(await screen.findByText('Avery Chen')).toBeInTheDocument()
    expect(await screen.findByText('Data Structures')).toBeInTheDocument()
    expect(screen.getByText(/CSE101 · Fall 2026-2027/)).toBeInTheDocument()
  })

  it('reads the enrollments from the student route, which the server scopes', async () => {
    renderDrawer()

    await waitFor(() => expect(mockedEnrollments).toHaveBeenCalledWith(51))
  })

  it('reads the attendance from the enrollment-scoped route', async () => {
    renderDrawer()

    await waitFor(() => expect(mockedAttendance).toHaveBeenCalledWith(300))
  })

  it('never calls the administrator-only user collection', async () => {
    renderDrawer()

    await screen.findByText('Avery Chen')
    expect(mockedUser).not.toHaveBeenCalled()
  })

  it('says the account is unavailable rather than asking for it', async () => {
    renderDrawer()

    expect(await screen.findByText(/not available for your role/i)).toBeInTheDocument()
  })

  it('never calls the administrator-only enrollment or attendance collections', async () => {
    renderDrawer()

    await screen.findByText('Avery Chen')
    // The old drawer read GET /enrollments and GET /attendance-records, which
    // are administrator only and answered 403 for a faculty member.
    expect(mockedAdminEnrollments).not.toHaveBeenCalled()
    expect(mockedAdminRecords).not.toHaveBeenCalled()
  })

  it('refuses a student who is outside the teaching scope of the caller', async () => {
    mockedEnrollments.mockRejectedValue(
      new ApiError(403, 'You cannot access this student record', null),
    )

    renderDrawer()

    expect(await screen.findByText(/outside your scope/i)).toBeInTheDocument()
    expect(mockedUser).not.toHaveBeenCalled()
  })

  // ------------------------------------------------------------------
  // Empty and state handling
  // ------------------------------------------------------------------

  it('says so when the student has no enrollments', async () => {
    mockedEnrollments.mockResolvedValue([])

    renderDrawer()

    expect(
      await screen.findByText(/no course enrollments are linked to this student profile/i),
    ).toBeInTheDocument()
  })

  it('says so when no attendance has been recorded', async () => {
    mockedAttendance.mockResolvedValue([])

    renderDrawer()

    expect(
      await screen.findByText(/no attendance has been recorded/i),
    ).toBeInTheDocument()
  })

  it('shows a loading state while the enrollments are being read', () => {
    mockedEnrollments.mockReturnValue(new Promise(() => {}) as never)

    renderDrawer()

    expect(screen.getByText(/loading enrollments/i)).toBeInTheDocument()
  })

  it('shows an error state when the request fails for a reason other than a refusal', async () => {
    mockedEnrollments.mockRejectedValue(new Error('boom'))

    renderDrawer()

    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })

  // ------------------------------------------------------------------
  // Request behaviour
  // ------------------------------------------------------------------

  it('does not repeat a request when the drawer is opened again', async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, gcTime: 0, staleTime: Infinity } },
    })

    const drawer = (shown: StudentResponse | null) => (
      <QueryClientProvider client={client}>
        <Student360Drawer
          student={shown}
          onOpenChange={() => {}}
          departmentName="Computer Science and Engineering"
        />
      </QueryClientProvider>
    )

    const { rerender } = render(drawer(student))

    await screen.findByText('Data Structures')
    const afterFirstOpen = mockedEnrollments.mock.calls.length

    // Close and reopen: the data is already cached, so it is reused rather than
    // fetched a second time.
    rerender(drawer(null))
    rerender(drawer(student))

    await screen.findByText('Data Structures')
    expect(mockedEnrollments.mock.calls.length).toBe(afterFirstOpen)
  })

  it('reads the attendance once per enrollment rather than once per row', async () => {
    mockedEnrollments.mockResolvedValue([
      enrollment({ enrollmentId: 300, courseId: 7 }),
      enrollment({ enrollmentId: 301, courseId: 8 }),
    ])
    mockedCourses.mockResolvedValue([course(), course({ courseId: 8, courseCode: 'CSE201' })])

    renderDrawer()

    await waitFor(() => expect(mockedAttendance).toHaveBeenCalledTimes(2))
    expect(mockedAttendance).toHaveBeenCalledWith(300)
    expect(mockedAttendance).toHaveBeenCalledWith(301)
  })
})
