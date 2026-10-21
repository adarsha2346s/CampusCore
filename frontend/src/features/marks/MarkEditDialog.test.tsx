import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MarkEditDialog } from './MarkEditDialog'
import { updateMark } from './marks.api'
import type { AssessmentResponse, MarkResponse } from '../../types/api'

vi.mock('./marks.api', () => ({
  updateMark: vi.fn(),
}))

const mockedUpdateMark = vi.mocked(updateMark)

const assessment: AssessmentResponse = {
  assessmentId: 42,
  courseId: 7,
  name: 'Midterm Exam',
  assessmentType: 'MIDTERM',
  maxMarks: 100,
  weight: 30,
  assessmentDate: '2026-03-12',
}

const mark: MarkResponse = {
  markId: 700,
  enrollmentId: 300,
  assessmentId: 42,
  marksObtained: '40.00',
  enteredAt: '2026-03-14T09:30:00',
}

describe('MarkEditDialog', () => {
  beforeEach(() => {
    mockedUpdateMark.mockReset()
  })

  /**
   * The dialog deliberately renders two controls with the same accessible name: a
   * visually hidden submit button inside the form so Enter works, and the visible
   * footer button. The hidden one is rendered first, so the footer button is the
   * last match.
   */
  const saveButton = () => {
    const buttons = screen.getAllByRole('button', { name: /^save mark$/i })
    return buttons[buttons.length - 1]
  }

  function open(overrides?: Partial<AssessmentResponse | null>) {
    render(
      <MarkEditDialog
        open
        onOpenChange={() => {}}
        mark={mark}
        assessment={{ ...assessment, ...(overrides ?? {}) }}
        context="Student 1 · Midterm Exam"
        onSubmit={async (id, request) => {
          await mockedUpdateMark(id, request)
        }}
      />,
    )
  }

  it('submits only the corrected value, never the enrollment or assessment', async () => {
    const user = userEvent.setup()
    mockedUpdateMark.mockResolvedValue({ ...mark, marksObtained: '55.00' })
    open()

    const input = screen.getByLabelText(/marks obtained/i)
    await user.clear(input)
    await user.type(input, '55.00')
    await user.click(saveButton())

    await vi.waitFor(() => expect(mockedUpdateMark).toHaveBeenCalledTimes(1))
    // Only marksObtained travels: re-pointing a grade would move it to another
    // student's row or collide with the one-mark-per-pair constraint.
    expect(mockedUpdateMark).toHaveBeenCalledWith(700, { marksObtained: 55 })
  })

  it('blocks submission and explains why when the value exceeds the assessment maximum', async () => {
    const user = userEvent.setup()
    open()

    const input = screen.getByLabelText(/marks obtained/i)
    await user.clear(input)
    await user.type(input, '120')

    expect(await screen.findByText(/cannot exceed the maximum of 100/i)).toBeInTheDocument()
    expect(saveButton()).toBeDisabled()
  })

  it('accepts a value equal to the maximum', async () => {
    const user = userEvent.setup()
    mockedUpdateMark.mockResolvedValue({ ...mark, marksObtained: '100.00' })
    open()

    const input = screen.getByLabelText(/marks obtained/i)
    await user.clear(input)
    await user.type(input, '100')

    await user.click(saveButton())

    await vi.waitFor(() =>
      expect(mockedUpdateMark).toHaveBeenCalledWith(700, { marksObtained: 100 }),
    )
  })

  it('rejects more than two decimal places before sending anything', async () => {
    const user = userEvent.setup()
    open()

    const input = screen.getByLabelText(/marks obtained/i)
    await user.clear(input)
    await user.type(input, '40.125')
    await user.click(saveButton())

    expect(await screen.findByText(/up to two decimals/i)).toBeInTheDocument()
    expect(mockedUpdateMark).not.toHaveBeenCalled()
  })

  it('shows the owning assessment and states that the grade cannot be moved', async () => {
    open()

    expect(screen.getAllByText(/midterm exam/i).length).toBeGreaterThan(0)
    expect(
      screen.getByText(/cannot be moved to a different student or assessment/i),
    ).toBeInTheDocument()
  })
})