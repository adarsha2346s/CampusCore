import { render, screen } from '@testing-library/react'
import { Toaster } from 'sonner'
import { describe, expect, it } from 'vitest'
import { notifyError } from './feedback'
import { ApiError } from '../../lib/api/api-error'

function renderToaster() {
  render(<Toaster />)
}

/**
 * How a rejected write reaches the administrator.
 *
 * This matters more since optimistic locking landed: a stale write is now
 * rejected with 409, and swallowing that would leave someone believing a grade or
 * an attendance record was saved when the server refused it. The fallback string
 * exists for genuinely unexpected failures, so the server's own message has to win
 * for the ones it explains.
 */
describe('notifyError', () => {
  it('surfaces the server message for a conflict', async () => {
    renderToaster()

    notifyError(
      new ApiError(409, 'The record was changed by someone else. Reload and try again.', null),
      'The mark could not be corrected.',
    )

    expect(
      await screen.findByText('The record was changed by someone else. Reload and try again.'),
    ).toBeInTheDocument()
  })

  it('uses the server message for other API failures too', async () => {
    renderToaster()

    notifyError(
      new ApiError(400, 'Marks cannot be greater than maximum marks', null),
      'The mark could not be corrected.',
    )

    expect(
      await screen.findByText('Marks cannot be greater than maximum marks'),
    ).toBeInTheDocument()
  })

  it('falls back to the caller wording when the failure is not an API error', async () => {
    renderToaster()

    notifyError(new Error('boom'), 'The mark could not be corrected.')

    expect(
      await screen.findByText('The mark could not be corrected.'),
    ).toBeInTheDocument()
  })
})
