import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../lib/api/http'
import {
  assignFacultyToCourse,
  getCourseAssignmentHistory,
  getCourseAssignments,
  reassignCourse,
  removeCourseAssignment,
} from './courseAssignments.api'

vi.mock('../../lib/api/http', () => ({ http: vi.fn() }))

const mockedHttp = vi.mocked(http)

/**
 * The exact requests the assignment workflow makes.
 *
 * The relationship hangs off the course, not off the faculty member, and the
 * three writes are distinguished only by their method and body. Getting any of
 * them wrong silently produces a 404 or, worse, a write to the wrong record, so
 * the paths are pinned here rather than left to a manual read of the client.
 */
describe('course assignment API client', () => {
  beforeEach(() => {
    mockedHttp.mockReset()
    mockedHttp.mockResolvedValue(undefined as never)
  })

  it('reads the current assignments from the course', () => {
    getCourseAssignments(7)

    expect(mockedHttp).toHaveBeenCalledTimes(1)
    expect(mockedHttp).toHaveBeenCalledWith('courses/7/faculty-assignments')
  })

  it('reads the assignment history from the course', () => {
    getCourseAssignmentHistory(7)

    expect(mockedHttp).toHaveBeenCalledTimes(1)
    expect(mockedHttp).toHaveBeenCalledWith('courses/7/faculty-assignments/history')
  })

  it('adds a faculty member with a POST carrying only the faculty id', () => {
    assignFacultyToCourse(7, 42)

    expect(mockedHttp).toHaveBeenCalledWith('courses/7/faculty-assignments', {
      method: 'POST',
      body: { facultyId: 42 },
    })
  })

  it('hands the course over with a PUT carrying only the faculty id', () => {
    reassignCourse(7, 42)

    expect(mockedHttp).toHaveBeenCalledWith('courses/7/faculty-assignments', {
      method: 'PUT',
      body: { facultyId: 42 },
    })
  })

  it('ends an assignment with a DELETE naming the faculty member', () => {
    removeCourseAssignment(7, 42)

    expect(mockedHttp).toHaveBeenCalledWith('courses/7/faculty-assignments/42', {
      method: 'DELETE',
    })
  })

  it('never sends a course id or a status the server did not ask for', () => {
    assignFacultyToCourse(7, 42)

    const body = mockedHttp.mock.calls[0][1]?.body as Record<string, unknown>
    expect(Object.keys(body)).toEqual(['facultyId'])
  })
})
