import type { ReactElement } from 'react'
import { render, type RenderResult } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

/**
 * Renders a component inside the providers every workspace page expects.
 *
 * Queries are created with retries disabled and caching off, so a test that
 * asserts a loading state actually observes it and a test that asserts real data
 * is not served from a previous test's cache.
 *
 * A caller that needs to observe caching itself can pass its own client, for
 * instance one with a long stale time, to check that a repeated interaction
 * reuses what was already fetched.
 */
export function renderWithProviders(ui: ReactElement, client?: QueryClient): RenderResult {
  const queryClient = client ?? new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0, staleTime: 0 },
      mutations: { retry: false },
    },
  })

  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

/** A student mark exactly as GET /api/v1/marks/me returns it. */
export const studentMark = (overrides: Record<string, unknown> = {}) => ({
  markId: 1,
  courseCode: 'CS201',
  courseName: 'Data Structures',
  semester: 'Fall',
  academicYear: '2026',
  assessmentName: 'Midterm Exam',
  assessmentType: 'MIDTERM',
  marksObtained: '42.50',
  maxMarks: '100.00',
  weight: '30.00',
  enteredAt: '2026-03-14T09:30:00',
  ...overrides,
})

/** A student attendance row exactly as GET /api/v1/attendance-records/me returns it. */
export const studentAttendance = (overrides: Record<string, unknown> = {}) => ({
  attendanceRecordId: 1,
  attendanceSessionId: 10,
  courseCode: 'CS201',
  courseName: 'Data Structures',
  sessionDate: '2026-02-09',
  topic: 'Trees',
  status: 'PRESENT',
  markedAt: '2026-02-09T10:00:00',
  ...overrides,
})