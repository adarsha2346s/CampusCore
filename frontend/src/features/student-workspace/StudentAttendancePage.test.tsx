import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { StudentAttendancePage } from './StudentAttendancePage'
import { getMyAttendance } from '../attendance/attendance.api'
import { renderWithProviders, studentAttendance } from '../../test/harness'

// The full module is mocked so that calling the administrator-only listing is
// observable. If the page regressed to GET /api/v1/attendance-sessions it would
// hit getAttendanceSessions, which the server refuses for every student.
vi.mock('../attendance/attendance.api', () => ({
  getMyAttendance: vi.fn(),
  getAttendanceSessions: vi.fn(),
  studentAttendanceKeys: { mine: ['student', 'attendance-records', 'me'] },
}))

const mockedGetMyAttendance = vi.mocked(getMyAttendance)
const mockedGetAttendanceSessions = vi.mocked(
  (await import('../attendance/attendance.api')).getAttendanceSessions,
)

describe('StudentAttendancePage', () => {
  beforeEach(() => {
    mockedGetMyAttendance.mockReset()
    mockedGetAttendanceSessions.mockReset()
  })

  it('uses the student-scoped route and never the administrator-only session listing', async () => {
    mockedGetMyAttendance.mockResolvedValue([studentAttendance()])

    renderWithProviders(<StudentAttendancePage />)

    await screen.findByText('Trees')

    expect(mockedGetMyAttendance).toHaveBeenCalledTimes(1)
    // Regression guard: this listing is ADMIN-only, and asking for it is what made
    // the page return 403 for every student instead of their own attendance.
    expect(mockedGetAttendanceSessions).not.toHaveBeenCalled()
  })

  it('shows the session topic, date and status from the server response', async () => {
    mockedGetMyAttendance.mockResolvedValue([studentAttendance()])

    renderWithProviders(<StudentAttendancePage />)

    await screen.findByText('Trees')
    expect(screen.getByText('2026-02-09')).toBeInTheDocument()
    expect(screen.getByText('PRESENT')).toBeInTheDocument()
    expect(screen.getByText('Data Structures')).toBeInTheDocument()
  })

  it('reports the attendance rate across the loaded rows', async () => {
    mockedGetMyAttendance.mockResolvedValue([
      studentAttendance({ attendanceRecordId: 1, topic: 'Week 1', status: 'PRESENT' }),
      studentAttendance({ attendanceRecordId: 2, topic: 'Week 2', status: 'PRESENT' }),
      studentAttendance({ attendanceRecordId: 3, topic: 'Week 3', status: 'ABSENT' }),
      studentAttendance({ attendanceRecordId: 4, topic: 'Week 4', status: 'ABSENT' }),
    ])

    renderWithProviders(<StudentAttendancePage />)

    await screen.findByText('Week 1')
    // Two present out of four records.
    expect(screen.getByText('50%')).toBeInTheDocument()
  })

  it('explains the empty state when nothing has been marked', async () => {
    mockedGetMyAttendance.mockResolvedValue([])

    renderWithProviders(<StudentAttendancePage />)

    expect(await screen.findByText('Nothing marked yet')).toBeInTheDocument()
  })

  it('labels a session that was recorded without a topic', async () => {
    mockedGetMyAttendance.mockResolvedValue([studentAttendance({ topic: null })])

    renderWithProviders(<StudentAttendancePage />)

    expect(await screen.findByText('Session without a topic')).toBeInTheDocument()
  })
})