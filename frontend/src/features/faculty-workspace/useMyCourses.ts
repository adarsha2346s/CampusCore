import { useQuery } from '@tanstack/react-query'
import { facultyCourseKeys, getFacultyCourses, getMyFacultyProfile } from '../faculty/faculty.api'
import type { FacultyAssignment } from '../faculty/faculty.api'

export interface MyCoursesResult {
  /** Courses the signed-in faculty member is currently assigned to. */
  courses: FacultyAssignment[]
  facultyId: number | undefined
  isPending: boolean
  isError: boolean
  error: unknown
  refetch: () => void
}

/**
 * Loads the signed-in faculty member's current course assignments.
 *
 * Shared by the faculty teaching pages so every one of them scopes its data the
 * same way: from the courses the server says this account is assigned to, never
 * from a list the browser chose.
 */
export function useMyCourses(): MyCoursesResult {
  const profile = useQuery({
    queryKey: ['faculty', 'self'],
    queryFn: getMyFacultyProfile,
  })

  const facultyId = profile.data?.facultyId
  const hasId = typeof facultyId === 'number'

  const courses = useQuery({
    queryKey: facultyCourseKeys.mine(facultyId ?? 0),
    queryFn: () => getFacultyCourses(facultyId as number),
    enabled: hasId,
  })

  return {
    courses: (courses.data ?? []).filter((course) => course.active),
    facultyId,
    isPending: profile.isPending || courses.isPending,
    isError: profile.isError || courses.isError,
    error: profile.error ?? courses.error,
    refetch: () => {
      void profile.refetch()
      void courses.refetch()
    },
  }
}

/**
 * Query keys for faculty teaching pages.
 *
 * Prefixed `faculty` rather than reusing the administrator-prefixed keys, so a
 * faculty-scoped read can never share a cache entry with an administrator one.
 */
export const facultyTeachingKeys = {
  enrollments: (courseId: number) => ['faculty', 'enrollments', courseId] as const,
  assessments: (courseId: number) => ['faculty', 'assessments', courseId] as const,
  marks: (enrollmentId: number) => ['faculty', 'marks', enrollmentId] as const,
  sessions: (courseId: number) => ['faculty', 'attendance-sessions', courseId] as const,
  records: (sessionId: number) => ['faculty', 'attendance-records', sessionId] as const,
}
