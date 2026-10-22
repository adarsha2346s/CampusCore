import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/harness'
import { CourseAssignmentsDialog } from './CourseAssignmentsDialog'
import {
  assignFacultyToCourse,
  getCourseAssignmentHistory,
  getCourseAssignments,
  reassignCourse,
  removeCourseAssignment,
} from './courseAssignments.api'
import { getFaculty } from '../faculty/faculty.api'
import type { CourseResponse, FacultyResponse } from '../../types/api'
import type { FacultyAssignment } from '../faculty/faculty.api'

vi.mock('./courseAssignments.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./courseAssignments.api')>()),
  getCourseAssignments: vi.fn(),
  getCourseAssignmentHistory: vi.fn(),
  assignFacultyToCourse: vi.fn(),
  reassignCourse: vi.fn(),
  removeCourseAssignment: vi.fn(),
}))

vi.mock('../faculty/faculty.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../faculty/faculty.api')>()),
  getFaculty: vi.fn(),
}))

const mockedAssignments = vi.mocked(getCourseAssignments)
const mockedHistory = vi.mocked(getCourseAssignmentHistory)
const mockedAssign = vi.mocked(assignFacultyToCourse)
const mockedReassign = vi.mocked(reassignCourse)
const mockedRemove = vi.mocked(removeCourseAssignment)
const mockedFaculty = vi.mocked(getFaculty)

const course: CourseResponse = {
  courseId: 7,
  departmentId: 1,
  courseCode: 'CSE101',
  courseName: 'Data Structures',
  credits: 4,
  capacity: 40,
  status: 'ACTIVE',
}

const facultyMember = (overrides: Partial<FacultyResponse> = {}): FacultyResponse => ({
  facultyId: 42,
  userId: 1042,
  departmentId: 1,
  employeeNumber: 'DEMO-FAC-001',
  firstName: 'Jamie',
  lastName: 'Patel',
  phone: null,
  status: 'ACTIVE',
  ...overrides,
})

const assignment = (overrides: Partial<FacultyAssignment> = {}): FacultyAssignment => ({
  assignmentId: 900,
  courseId: 7,
  courseCode: 'CSE101',
  courseName: 'Data Structures',
  facultyId: 42,
  employeeNumber: 'DEMO-FAC-001',
  facultyName: 'Jamie Patel',
  assignedAt: '2026-08-10T09:00:00',
  endedAt: null,
  active: true,
  ...overrides,
})

function renderDialog() {
  return renderWithProviders(
    <CourseAssignmentsDialog open onOpenChange={() => {}} course={course} />,
  )
}

describe('CourseAssignmentsDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedAssignments.mockResolvedValue([])
    mockedHistory.mockResolvedValue([])
    mockedFaculty.mockResolvedValue([facultyMember()])
  })

  // ------------------------------------------------------------------
  // Listing
  // ------------------------------------------------------------------

  it('lists the faculty currently assigned to the course', async () => {
    mockedAssignments.mockResolvedValue([
      assignment({ assignmentId: 900, facultyId: 42, facultyName: 'Jamie Patel' }),
      assignment({ assignmentId: 901, facultyId: 43, facultyName: 'Morgan Rivera', employeeNumber: 'DEMO-FAC-002' }),
    ])

    renderDialog()

    expect(await screen.findByText('Jamie Patel')).toBeInTheDocument()
    expect(screen.getByText('Morgan Rivera')).toBeInTheDocument()
    expect(screen.getByText('DEMO-FAC-001')).toBeInTheDocument()
  })

  it('says so when the course has nobody assigned', async () => {
    renderDialog()

    expect(await screen.findByText(/no faculty assigned yet/i)).toBeInTheDocument()
  })

  it('shows a loading state while the assignments are being read', () => {
    mockedAssignments.mockReturnValue(new Promise(() => {}) as never)

    renderDialog()

    expect(screen.getByText(/loading assigned faculty/i)).toBeInTheDocument()
  })

  it('shows an error state with a retry when the assignments cannot be read', async () => {
    mockedAssignments.mockRejectedValue(new Error('boom'))

    renderDialog()

    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })

  it('shows the previous assignments as history', async () => {
    mockedHistory.mockResolvedValue([
      assignment({
        assignmentId: 800,
        facultyId: 41,
        facultyName: 'Alex Stone',
        assignedAt: '2025-08-10T09:00:00',
        endedAt: '2026-08-09T09:00:00',
        active: false,
      }),
    ])

    renderDialog()

    expect(await screen.findByText('Alex Stone')).toBeInTheDocument()
  })

  // ------------------------------------------------------------------
  // Offering only assignable faculty
  // ------------------------------------------------------------------

  it('does not offer a faculty member who already holds an active assignment', async () => {
    mockedAssignments.mockResolvedValue([assignment({ facultyId: 42 })])
    mockedFaculty.mockResolvedValue([
      facultyMember({ facultyId: 42 }),
      facultyMember({ facultyId: 43, employeeNumber: 'DEMO-FAC-002', firstName: 'Morgan', lastName: 'Rivera' }),
    ])

    renderDialog()

    const select = await screen.findByRole('combobox', { name: /faculty member/i })
    const options = Array.from((select as HTMLSelectElement).options).map((option) => option.value)

    expect(options).not.toContain('42')
    expect(options).toContain('43')
  })

  it('does not offer an inactive faculty member', async () => {
    mockedFaculty.mockResolvedValue([
      facultyMember({ facultyId: 42, status: 'INACTIVE' }),
      facultyMember({ facultyId: 43, employeeNumber: 'DEMO-FAC-002' }),
    ])

    renderDialog()

    const select = await screen.findByRole('combobox', { name: /faculty member/i })
    const options = Array.from((select as HTMLSelectElement).options).map((option) => option.value)

    expect(options).not.toContain('42')
    expect(options).toContain('43')
  })

  // ------------------------------------------------------------------
  // Writing
  // ------------------------------------------------------------------

  it('assigns the selected faculty member to the course', async () => {
    mockedFaculty.mockResolvedValue([
      facultyMember({ facultyId: 42 }),
      facultyMember({ facultyId: 43, employeeNumber: 'DEMO-FAC-002' }),
    ])
    mockedAssign.mockResolvedValue(assignment({ facultyId: 43 }))

    const user = userEvent.setup()
    renderDialog()

    await user.selectOptions(await screen.findByRole('combobox', { name: /faculty member/i }), '43')
    await user.click(screen.getByRole('button', { name: /assign faculty/i }))

    await waitFor(() => expect(mockedAssign).toHaveBeenCalledTimes(1))
    expect(mockedAssign).toHaveBeenCalledWith(7, 43)
  })

  it('refetches the assignment list after a successful assignment', async () => {
    mockedFaculty.mockResolvedValue([
      facultyMember({ facultyId: 42 }),
      facultyMember({ facultyId: 43, employeeNumber: 'DEMO-FAC-002' }),
    ])
    mockedAssign.mockResolvedValue(assignment({ facultyId: 43 }))

    const user = userEvent.setup()
    renderDialog()

    await screen.findByText(/no faculty assigned yet/i)
    const before = mockedAssignments.mock.calls.length

    await user.selectOptions(await screen.findByRole('combobox', { name: /faculty member/i }), '43')
    await user.click(screen.getByRole('button', { name: /assign faculty/i }))

    await waitFor(() =>
      expect(mockedAssignments.mock.calls.length).toBeGreaterThan(before),
    )
  })

  it('blocks a second submission while the first one is in flight', async () => {
    mockedFaculty.mockResolvedValue([
      facultyMember({ facultyId: 42 }),
      facultyMember({ facultyId: 43, employeeNumber: 'DEMO-FAC-002' }),
    ])
    let release: (() => void) | undefined
    mockedAssign.mockReturnValue(new Promise((resolve) => {
      release = () => resolve(assignment({ facultyId: 43 }))
    }) as never)

    const user = userEvent.setup()
    renderDialog()

    await user.selectOptions(await screen.findByRole('combobox', { name: /faculty member/i }), '43')
    const button = screen.getByRole('button', { name: /assign faculty/i })
    await user.click(button)

    await waitFor(() => expect(button).toBeDisabled())
    release?.()
  })

  it('cannot submit without choosing a faculty member', async () => {
    renderDialog()

    const button = await screen.findByRole('button', { name: /assign faculty/i })
    expect(button).toBeDisabled()
    expect(mockedAssign).not.toHaveBeenCalled()
  })

  it('hands the course over through the reassign action', async () => {
    mockedAssignments.mockResolvedValue([assignment({ facultyId: 42 })])
    mockedFaculty.mockResolvedValue([
      facultyMember({ facultyId: 42 }),
      facultyMember({ facultyId: 43, employeeNumber: 'DEMO-FAC-002' }),
    ])
    mockedReassign.mockResolvedValue(assignment({ facultyId: 43 }))

    const user = userEvent.setup()
    renderDialog()

    await user.selectOptions(await screen.findByRole('combobox', { name: /faculty member/i }), '43')
    await user.click(screen.getByRole('button', { name: /reassign course/i }))

    const confirm = await screen.findByRole('button', { name: /reassign course/i })
    await user.click(confirm)

    await waitFor(() => expect(mockedReassign).toHaveBeenCalledTimes(1))
    expect(mockedReassign).toHaveBeenCalledWith(7, 43)
  })

  it('ends an assignment through the confirmed remove action', async () => {
    mockedAssignments.mockResolvedValue([assignment({ facultyId: 42 })])
    mockedRemove.mockResolvedValue(undefined)

    const user = userEvent.setup()
    renderDialog()

    await user.click(await screen.findByRole('button', { name: /end assignment for jamie patel/i }))

    const confirm = await screen.findByRole('button', { name: /end assignment/i })
    await user.click(confirm)

    await waitFor(() => expect(mockedRemove).toHaveBeenCalledTimes(1))
    expect(mockedRemove).toHaveBeenCalledWith(7, 42)
  })

  it('surfaces the server reason when the server refuses to remove the last assignment', async () => {
    mockedAssignments.mockResolvedValue([assignment({ facultyId: 42 })])
    mockedRemove.mockRejectedValue(
      new (await import('../../lib/api/api-error')).ApiError(
        409,
        'Course CSE101 still has enrolled students. Reassign the course to another faculty member instead of removing the last assignment.',
        null,
      ),
    )

    const user = userEvent.setup()
    renderDialog()

    await user.click(await screen.findByRole('button', { name: /end assignment for jamie patel/i }))
    const confirm = await screen.findByRole('button', { name: /end assignment/i })
    await user.click(confirm)

    expect(
      await screen.findByText(/reassign the course to another faculty member/i),
    ).toBeInTheDocument()
  })
})
