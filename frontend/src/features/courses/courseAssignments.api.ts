import { http } from '../../lib/api/http'
import type { FacultyAssignment } from '../faculty/faculty.api'

/**
 * Administrator endpoints for faculty-to-course assignments.
 *
 * The paths mirror the backend exactly: the relationship hangs off the course,
 * not off the faculty member. Reads are open to any signed-in user, because who
 * teaches a course is shown to students; writes are ADMIN only and the server
 * enforces that regardless of what the interface offers.
 */
export const courseAssignmentKeys = {
  active: (courseId: number) => ['admin', 'course-assignments', courseId, 'active'] as const,
  history: (courseId: number) => ['admin', 'course-assignments', courseId, 'history'] as const,
}

/** Faculty members currently assigned to the course. */
export const getCourseAssignments = (courseId: number) =>
  http<FacultyAssignment[]>(`courses/${courseId}/faculty-assignments`)

/** Every assignment for the course, including the ones that have ended. */
export const getCourseAssignmentHistory = (courseId: number) =>
  http<FacultyAssignment[]>(`courses/${courseId}/faculty-assignments/history`)

/**
 * Adds a faculty member to the course.
 *
 * A course may be co-taught, so this adds an assignment without closing the
 * existing ones. The server rejects it when that faculty member is already
 * actively assigned to the course.
 */
export const assignFacultyToCourse = (courseId: number, facultyId: number) =>
  http<FacultyAssignment>(`courses/${courseId}/faculty-assignments`, {
    method: 'POST',
    body: { facultyId },
  })

/**
 * Hands the course over to a different faculty member.
 *
 * The previous assignment is closed rather than deleted, so the teaching
 * history survives and the enrollments, assessments, marks and attendance
 * records stay exactly as they are.
 */
export const reassignCourse = (courseId: number, facultyId: number) =>
  http<FacultyAssignment>(`courses/${courseId}/faculty-assignments`, {
    method: 'PUT',
    body: { facultyId },
  })

/**
 * Ends an assignment.
 *
 * The row is closed rather than deleted. The server refuses to remove the last
 * assignment of a course that still has enrolled students, because that would
 * leave those students without an assigned faculty member; reassign instead.
 */
export const removeCourseAssignment = (courseId: number, facultyId: number) =>
  http<void>(`courses/${courseId}/faculty-assignments/${facultyId}`, {
    method: 'DELETE',
  })
