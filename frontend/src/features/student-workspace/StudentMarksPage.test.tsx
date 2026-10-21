import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { StudentMarksPage } from './StudentMarksPage'
import { getMyMarks } from '../marks/marks.api'
import { renderWithProviders, studentMark } from '../../test/harness'

vi.mock('../marks/marks.api', () => ({
  getMyMarks: vi.fn(),
  studentMarkKeys: { mine: ['student', 'marks', 'me'] },
}))

const mockedGetMyMarks = vi.mocked(getMyMarks)

describe('StudentMarksPage', () => {
  beforeEach(() => {
    mockedGetMyMarks.mockReset()
  })

  it('reads the student-scoped route rather than fanning out per enrollment', async () => {
    mockedGetMyMarks.mockResolvedValue([studentMark()])

    renderWithProviders(<StudentMarksPage />)

    await screen.findByText('Midterm Exam')

    // Exactly one call: the page no longer issues a request per enrollment.
    expect(mockedGetMyMarks).toHaveBeenCalledTimes(1)
  })

  it('shows the course, assessment and mark with the server supplied labels', async () => {
    mockedGetMyMarks.mockResolvedValue([studentMark()])

    renderWithProviders(<StudentMarksPage />)

    await screen.findByText('Midterm Exam')
    expect(screen.getByText('Data Structures')).toBeInTheDocument()
    expect(screen.getByText('CS201')).toBeInTheDocument()
    expect(screen.getByText('42.50')).toBeInTheDocument()
    expect(screen.getByText('Fall 2026')).toBeInTheDocument()
  })

  it('shows the percentage of the assessment maximum alongside the raw value', async () => {
    mockedGetMyMarks.mockResolvedValue([studentMark({ marksObtained: '42.50', maxMarks: '50.00' })])

    renderWithProviders(<StudentMarksPage />)

    // 42.5 of 50 is 85%, which is the figure a student actually reads.
    expect(await screen.findByText('85%')).toBeInTheDocument()
  })

  it('omits the percentage rather than showing a wrong one when the maximum is unusable', async () => {
    mockedGetMyMarks.mockResolvedValue([studentMark({ marksObtained: '42.50', maxMarks: '0' })])

    renderWithProviders(<StudentMarksPage />)

    await screen.findByText('Midterm Exam')
    expect(screen.queryByText('%')).not.toBeInTheDocument()
  })

  it('explains the empty state when no marks have been recorded', async () => {
    mockedGetMyMarks.mockResolvedValue([])

    renderWithProviders(<StudentMarksPage />)

    expect(await screen.findByText('No marks recorded yet')).toBeInTheDocument()
  })

  it('surfaces a failure instead of rendering an empty table', async () => {
    mockedGetMyMarks.mockRejectedValue(new Error('Network down'))

    renderWithProviders(<StudentMarksPage />)

    expect(await screen.findByRole('button', { name: /try again/i })).toBeInTheDocument()
    expect(screen.queryByText('No marks recorded yet')).not.toBeInTheDocument()
  })
})