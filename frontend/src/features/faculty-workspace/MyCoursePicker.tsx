import { SelectField } from '../../components/forms/SelectField'
import { EmptyState } from '../../components/data-display/EmptyState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { useMyCourses } from './useMyCourses'

interface MyCoursePickerProps {
  selectedCourseId: number | null
  onSelect: (courseId: number) => void
  /** Sentence describing what the selected course will scope. */
  scopingHint?: string
}

/**
 * Course selector limited to the signed-in faculty member's assignments.
 *
 * There is no free-text input and no "all courses" option: the only choices are
 * courses the server reports this account as assigned to, which is what keeps
 * every downstream request inside the faculty teaching scope.
 */
export function MyCoursePicker({
  selectedCourseId,
  onSelect,
  scopingHint,
}: MyCoursePickerProps) {
  const { courses, isPending } = useMyCourses()

  if (isPending) {
    return <LoadingState label="Loading your assigned courses" />
  }

  if (courses.length === 0) {
    return (
      <EmptyState
        title="No courses assigned yet"
        description="An administrator assigns faculty to courses. Teaching data appears here once you are assigned."
      />
    )
  }

  const activeId = selectedCourseId ?? courses[0].courseId

  return (
    <div className="faculty-scope">
      <SelectField
        id="faculty-course"
        name="faculty-course"
        label="Course"
        value={String(activeId)}
        onChange={(event) => onSelect(Number(event.target.value))}
      >
        {courses.map((course) => (
          <option key={course.courseId} value={course.courseId}>
            {course.courseCode} — {course.courseName}
          </option>
        ))}
      </SelectField>
      <p className="faculty-scope__hint">
        {scopingHint ?? 'Only courses you are assigned to can be selected.'}
      </p>
    </div>
  )
}
