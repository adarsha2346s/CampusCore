import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { UserRound, Users } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Dialog } from '../../components/ui/Dialog'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { SelectField } from '../../components/forms/SelectField'
import { notifyError } from '../admin-shared/feedback'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { facultyCourseKeys, facultyKeys, getFaculty } from '../faculty/faculty.api'
import type { FacultyAssignment } from '../faculty/faculty.api'
import { formatDate } from '../../lib/format/datetime'
import {
  assignFacultyToCourse,
  courseAssignmentKeys,
  getCourseAssignmentHistory,
  getCourseAssignments,
  reassignCourse,
  removeCourseAssignment,
} from './courseAssignments.api'
import type { CourseResponse } from '../../types/api'

interface CourseAssignmentsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  course: CourseResponse | null
}

/**
 * Teaching staff for one course.
 *
 * An assignment is what gives a faculty member access to a course's
 * assessments, marks, attendance and enrolled students, so this is where an
 * administrator establishes that link.
 *
 * The relationship is append-only. Removing an assignment ends it, and a
 * handover closes the previous row instead of deleting it, so the record of who
 * taught a course before is never lost. The server refuses to remove the last
 * assignment of a course that still has enrolled students; the message it
 * returns is surfaced unchanged so the reason is not guessed at.
 */
export function CourseAssignmentsDialog({
  open,
  onOpenChange,
  course,
}: CourseAssignmentsDialogProps) {
  const client = useQueryClient()
  const courseId = course?.courseId ?? 0

  const [selectedFacultyId, setSelectedFacultyId] = useState('')

  const assignments = useQuery({
    queryKey: courseAssignmentKeys.active(courseId),
    queryFn: () => getCourseAssignments(courseId),
    enabled: open && Boolean(course),
  })

  const history = useQuery({
    queryKey: courseAssignmentKeys.history(courseId),
    queryFn: () => getCourseAssignmentHistory(courseId),
    enabled: open && Boolean(course),
  })

  const faculty = useQuery({
    queryKey: facultyKeys.all,
    queryFn: getFaculty,
    enabled: open,
  })

  const active = useMemo(
    () => (assignments.data ?? []).filter((assignment) => assignment.active),
    [assignments.data],
  )

  const assignedFacultyIds = useMemo(
    () => new Set(active.map((assignment) => assignment.facultyId)),
    [active],
  )

  /**
   * Faculty who can be picked: an inactive member is refused by the server, and
   * a member who already holds an active assignment would be rejected as a
   * duplicate. Neither belongs in the list, so neither is offered.
   */
  const selectable = useMemo(
    () =>
      (faculty.data ?? []).filter(
        (member) => member.status === 'ACTIVE' && !assignedFacultyIds.has(member.facultyId),
      ),
    [faculty.data, assignedFacultyIds],
  )

  const selectedFaculty = selectable.find(
    (member) => String(member.facultyId) === selectedFacultyId,
  )

  /**
   * Refreshes everything an assignment change can affect: the two assignment
   * listings for this course, and the course list of every faculty member
   * involved, because that list is what the faculty workspace reads.
   */
  const refresh = async (facultyIds: number[]) => {
    await Promise.all([
      client.invalidateQueries({ queryKey: courseAssignmentKeys.active(courseId) }),
      client.invalidateQueries({ queryKey: courseAssignmentKeys.history(courseId) }),
      ...facultyIds.map((facultyId) =>
        client.invalidateQueries({ queryKey: facultyCourseKeys.mine(facultyId) }),
      ),
    ])
  }

  const assign = useMutation({
    mutationFn: (facultyId: number) => assignFacultyToCourse(courseId, facultyId),
    onSuccess: async (_result, facultyId) => {
      await refresh([facultyId])
      setSelectedFacultyId('')
      toast.success('Faculty member assigned to the course')
    },
  })

  const reassign = useMutation({
    mutationFn: (facultyId: number) => reassignCourse(courseId, facultyId),
    onSuccess: async (_result, facultyId) => {
      await refresh([facultyId, ...active.map((assignment) => assignment.facultyId)])
      setSelectedFacultyId('')
      toast.success('Course reassigned')
    },
  })

  const remove = useMutation({
    mutationFn: (facultyId: number) => removeCourseAssignment(courseId, facultyId),
    onSuccess: async (_result, facultyId) => {
      await refresh([facultyId])
      toast.success('Assignment ended')
    },
  })

  const busy = assign.isPending || reassign.isPending || remove.isPending

  async function assignSelected() {
    if (!selectedFaculty) return
    try {
      await assign.mutateAsync(selectedFaculty.facultyId)
    } catch (error) {
      notifyError(error, 'The faculty member could not be assigned to the course.')
    }
  }

  const columns = [
    {
      key: 'faculty',
      header: 'Faculty member',
      render: (assignment: FacultyAssignment) => (
        <div className="identity-cell">
          <span className="entity-icon entity-icon--blue" aria-hidden="true">
            <UserRound size={18} />
          </span>
          <span>
            <strong>{assignment.facultyName}</strong>
            <small className="mono-label">{assignment.employeeNumber}</small>
          </span>
        </div>
      ),
    },
    {
      key: 'assigned',
      header: 'Assigned',
      render: (assignment: FacultyAssignment) => formatDate(assignment.assignedAt) ?? '—',
    },
    {
      key: 'status',
      header: 'Status',
      render: (assignment: FacultyAssignment) => (
        <StatusBadge status={assignment.active ? 'ACTIVE' : 'ENDED'} />
      ),
    },
  ]

  const closedAssignments = (history.data ?? []).filter((assignment) => !assignment.active)

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Teaching staff"
      description={
        course ? `${course.courseCode} — ${course.courseName}` : 'Select a course to manage.'
      }
      footer={
        <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={busy}>
          Close
        </Button>
      }
    >
      {!course ? null : (
        <>
          <section aria-labelledby="course-assignments-current">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Current</p>
                <h2 id="course-assignments-current">Assigned faculty</h2>
              </div>
            </div>

            {assignments.isPending ? (
              <LoadingState label="Loading assigned faculty" />
            ) : assignments.isError ? (
              <ErrorState
                error={assignments.error}
                onRetry={() => void assignments.refetch()}
              />
            ) : active.length === 0 ? (
              <EmptyState
                title="No faculty assigned yet"
                description="Until somebody is assigned, nobody can reach this course's assessments, marks, attendance or enrolled students."
              />
            ) : (
              <ResourceTable
                caption="Faculty currently assigned to this course"
                columns={columns}
                rows={active}
                getRowKey={(assignment) => assignment.assignmentId}
                actions={(assignment) => (
                  <div className="row-actions">
                    <ConfirmDialog
                      title="End this assignment?"
                      description={`End ${assignment.facultyName}'s assignment to ${course.courseCode}? The record is kept as history, but they lose access to this course's teaching data. If they are the only assigned faculty member and the course still has enrolled students, the server will refuse and ask you to reassign instead.`}
                      confirmLabel="End assignment"
                      onConfirm={async () => {
                        await remove.mutateAsync(assignment.facultyId)
                      }}
                      trigger={
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label={`End assignment for ${assignment.facultyName}`}
                          title="End assignment"
                          disabled={busy}
                        >
                          End
                        </Button>
                      }
                    />
                  </div>
                )}
              />
            )}
          </section>

          <section aria-labelledby="course-assignments-manage" className="faculty-scope">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Manage</p>
                <h2 id="course-assignments-manage">Assign a faculty member</h2>
              </div>
            </div>

            {faculty.isPending ? (
              <LoadingState label="Loading faculty members" />
            ) : faculty.isError ? (
              <ErrorState error={faculty.error} onRetry={() => void faculty.refetch()} />
            ) : (
              <>
                <SelectField
                  id="course-assignment-faculty"
                  name="course-assignment-faculty"
                  label="Faculty member"
                  value={selectedFacultyId}
                  onChange={(event) => setSelectedFacultyId(event.target.value)}
                  disabled={busy || selectable.length === 0}
                >
                  <option value="">
                    {selectable.length === 0
                      ? 'No further faculty members are available'
                      : 'Select a faculty member'}
                  </option>
                  {selectable.map((member) => (
                    <option key={member.facultyId} value={member.facultyId}>
                      {member.firstName} {member.lastName ?? ''} — {member.employeeNumber}
                    </option>
                  ))}
                </SelectField>

                <p className="form-hint">
                  {selectable.length === 0
                    ? 'Every active faculty member already holds an assignment to this course.'
                    : 'A course may be co-taught, so adding somebody keeps the current faculty in place.'}
                </p>

                <div className="row-actions">
                  <Button
                    onClick={() => void assignSelected()}
                    disabled={busy || !selectedFaculty}
                  >
                    {assign.isPending
                      ? 'Assigning…'
                      : active.length === 0
                        ? 'Assign faculty'
                        : 'Add as co-teacher'}
                  </Button>

                  {active.length > 0 && (
                    <ConfirmDialog
                      title="Reassign this course?"
                      description={`Hand ${course.courseCode} over to ${
                        selectedFaculty
                          ? `${selectedFaculty.firstName} ${selectedFaculty.lastName ?? ''}`
                          : 'the selected faculty member'
                      }? The current assignment is closed rather than deleted, and the enrolled students, assessments, marks and attendance records are left untouched. The previous faculty member loses access to this course's teaching data.`}
                      confirmLabel="Reassign course"
                      onConfirm={async () => {
                        if (!selectedFaculty) return
                        await reassign.mutateAsync(selectedFaculty.facultyId)
                      }}
                      trigger={
                        <Button
                          variant="secondary"
                          disabled={busy || !selectedFaculty}
                        >
                          Reassign course
                        </Button>
                      }
                    />
                  )}
                </div>
              </>
            )}
          </section>

          <section aria-labelledby="course-assignments-history">
            <div className="section-heading">
              <div>
                <p className="eyebrow">History</p>
                <h2 id="course-assignments-history">Previous assignments</h2>
              </div>
            </div>

            {history.isPending ? (
              <LoadingState label="Loading assignment history" />
            ) : history.isError ? (
              <ErrorState error={history.error} onRetry={() => void history.refetch()} />
            ) : closedAssignments.length === 0 ? (
              <p className="form-hint">This course has never changed hands.</p>
            ) : (
              <ul className="data-list">
                {closedAssignments.map((assignment) => (
                  <li className="data-list__row" key={assignment.assignmentId}>
                    <span className="data-list__copy">
                      <strong>{assignment.facultyName}</strong>
                      <small className="mono-label">{assignment.employeeNumber}</small>
                    </span>
                    <span className="data-list__tag data-list__tag--value">
                      <Users size={14} aria-hidden="true" />{' '}
                      {formatDate(assignment.endedAt) ?? '—'}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </Dialog>
  )
}
