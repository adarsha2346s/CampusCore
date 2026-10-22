import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/harness'
import { CoursesPage } from './CoursesPage'
import { getCoursesPage } from './courses.api'
import { getDepartments } from '../departments/departments.api'
import { getFaculty } from '../faculty/faculty.api'
import { getCourseAssignmentHistory, getCourseAssignments } from './courseAssignments.api'
import type { CourseResponse, Department, PageResponse } from '../../types/api'

vi.mock('./courses.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./courses.api')>()),
  getCoursesPage: vi.fn(),
  createCourse: vi.fn(),
  updateCourse: vi.fn(),
  deleteCourse: vi.fn(),
  deactivateCourse: vi.fn(),
}))

vi.mock('../departments/departments.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../departments/departments.api')>()),
  getDepartments: vi.fn(),
}))

vi.mock('../faculty/faculty.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../faculty/faculty.api')>()),
  getFaculty: vi.fn(),
}))

vi.mock('./courseAssignments.api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./courseAssignments.api')>()),
  getCourseAssignments: vi.fn(),
  getCourseAssignmentHistory: vi.fn(),
  assignFacultyToCourse: vi.fn(),
  reassignCourse: vi.fn(),
  removeCourseAssignment: vi.fn(),
}))

const mockedCourses = vi.mocked(getCoursesPage)
const mockedDepartments = vi.mocked(getDepartments)
const mockedFaculty = vi.mocked(getFaculty)
const mockedAssignments = vi.mocked(getCourseAssignments)
const mockedHistory = vi.mocked(getCourseAssignmentHistory)

const course: CourseResponse = {
  courseId: 7,
  departmentId: 1,
  courseCode: 'CSE101',
  courseName: 'Data Structures',
  credits: 4,
  capacity: 40,
  status: 'ACTIVE',
}

const department: Department = {
  departmentId: 1,
  name: 'Computer Science and Engineering',
  code: 'CSE',
}

function pageOf<T>(content: T[]): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
  }
}

describe('CoursesPage assignment entry point', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedCourses.mockResolvedValue(pageOf([course]))
    mockedDepartments.mockResolvedValue([department])
    mockedFaculty.mockResolvedValue([])
    mockedAssignments.mockResolvedValue([])
    mockedHistory.mockResolvedValue([])
  })

  /**
   * The assignment workflow belongs to the administrator's course management
   * page, which the router places behind the ADMIN role guard. The action is
   * therefore only ever reachable from the admin workspace; the server refuses
   * the writes for anyone else regardless of what the interface offers.
   */
  it('offers a teaching staff action for each course', async () => {
    renderWithProviders(<CoursesPage />)

    expect(
      await screen.findByRole('button', { name: /teaching staff for CSE101/i }),
    ).toBeInTheDocument()
  })

  it('opens the assignment management for the chosen course', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CoursesPage />)

    await user.click(await screen.findByRole('button', { name: /teaching staff for CSE101/i }))

    expect(await screen.findByRole('heading', { name: /teaching staff/i })).toBeInTheDocument()
    expect(screen.getByText('CSE101 — Data Structures')).toBeInTheDocument()
    await waitFor(() => expect(mockedAssignments).toHaveBeenCalledWith(7))
  })
})
