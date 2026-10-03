import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye } from 'lucide-react'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { PaginationControls } from '../../components/data-display/PaginationControls'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { departmentKeys, getDepartments } from '../departments/departments.api'
import { courseKeys, getCoursesPage } from '../courses/courses.api'
import type { CourseResponse } from '../../types/api'

export function StudentCatalogPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const pageSize = 20
  const [selectedCourse, setSelectedCourse] = useState<CourseResponse | null>(null)
  const courses = useQuery({ queryKey: courseKeys.page(page, pageSize), queryFn: () => getCoursesPage(page, pageSize) })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const departmentById = useMemo(() => new Map((departments.data ?? []).map((department) => [department.departmentId, department.name])), [departments.data])
  const filtered = useMemo(() => (courses.data?.content ?? []).filter((course) => `${course.courseCode} ${course.courseName} ${departmentById.get(course.departmentId) ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())), [courses.data, departmentById, search])
  return (
    <div className="admin-page">
      <PageHeader eyebrow="Campus catalog" title="Course catalog" description="Browse course descriptions published in the campus catalog. Your personal enrollments are not available through this view." />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search this page" countLabel={`${courses.data?.totalElements ?? 0} courses total`} />
        {courses.isPending || departments.isPending ? <TableSkeleton rows={8} columns={3} label="Loading course catalog" />
          : courses.isError ? <ErrorState error={courses.error} onRetry={() => void courses.refetch()} />
            : departments.isError ? <ErrorState error={departments.error} onRetry={() => void departments.refetch()} />
              : filtered.length === 0 ? <EmptyState title={courses.data.totalElements ? 'No courses match this page search' : 'No courses available'} description={courses.data.totalElements ? 'Try another search or use the page controls.' : 'The campus catalog has no courses to show yet.'} />
                : <ResourceTable caption="Campus course catalog" columns={[
                  { key: 'course', header: 'Course', render: (course: CourseResponse) => <div className="identity-cell"><span className="identity-avatar" aria-hidden="true">{course.courseCode.slice(0, 1)}</span><span><strong>{course.courseName}</strong><small>{course.courseCode}</small></span></div> },
                  { key: 'department', header: 'Department', render: (course: CourseResponse) => departmentById.get(course.departmentId) ?? 'Department unavailable' },
                  { key: 'credits', header: 'Credits', render: (course: CourseResponse) => course.credits },
                ]} rows={filtered} getRowKey={(course) => course.courseId} actions={(course) => <Button size="sm" variant="ghost" title="View course details" aria-label={`View ${course.courseCode} details`} onClick={() => setSelectedCourse(course)}><Eye size={16} aria-hidden="true" /></Button>} />}
        {courses.data && <PaginationControls page={courses.data.page} size={courses.data.size} totalElements={courses.data.totalElements} totalPages={courses.data.totalPages} onPageChange={setPage} />}
      </Card>
      {selectedCourse && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setSelectedCourse(null) }} title={selectedCourse.courseName} description="Published course catalog details" fields={[
        { label: 'Course code', value: selectedCourse.courseCode },
        { label: 'Department', value: departmentById.get(selectedCourse.departmentId) },
        { label: 'Credits', value: selectedCourse.credits },
        { label: 'Capacity', value: selectedCourse.capacity },
        { label: 'Status', value: selectedCourse.status },
      ]} />}
    </div>
  )
}
