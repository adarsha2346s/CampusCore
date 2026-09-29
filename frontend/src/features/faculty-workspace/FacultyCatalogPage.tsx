import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye } from 'lucide-react'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { PaginationControls } from '../../components/data-display/PaginationControls'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { departmentKeys, getDepartments } from '../departments/departments.api'
import { courseKeys, getCoursesPage } from '../courses/courses.api'
import type { CourseResponse, Department } from '../../types/api'

export function FacultyCatalogPage({ kind }: { kind: 'courses' | 'departments' }) {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const pageSize = 20
  const [selectedCourse, setSelectedCourse] = useState<CourseResponse | null>(null)
  const courses = useQuery({ queryKey: courseKeys.page(page, pageSize), queryFn: () => getCoursesPage(page, pageSize), enabled: kind === 'courses' })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments, enabled: kind === 'departments' || kind === 'courses' })
  const departmentById = useMemo(() => new Map((departments.data ?? []).map((item) => [item.departmentId, item])), [departments.data])
  const courseRows = useMemo(() => (courses.data?.content ?? []).filter((course) => `${course.courseCode} ${course.courseName} ${departmentById.get(course.departmentId)?.name ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())), [courses.data, departmentById, search])
  const departmentRows = useMemo(() => (departments.data ?? []).filter((department) => `${department.name} ${department.code}`.toLowerCase().includes(search.trim().toLowerCase())), [departments.data, search])
  const loading = kind === 'courses' ? courses.isPending || departments.isPending : departments.isPending
  const error = kind === 'courses' ? courses.error ?? departments.error : departments.error
  const retry = () => { if (kind === 'courses') void courses.refetch(); void departments.refetch() }

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic catalog" title={kind === 'courses' ? 'Course catalog' : 'Departments'} description={kind === 'courses' ? 'Browse the campus course catalog. These entries are not faculty assignment information.' : 'Browse the campus department directory.'} />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel={kind === 'courses' ? 'Search this page' : 'Search departments'} countLabel={kind === 'courses' ? `${courses.data?.totalElements ?? 0} courses total` : `${departmentRows.length} of ${departments.data?.length ?? 0} departments`} />
        {loading ? <LoadingState label={`Loading ${kind}`} />
          : error ? <ErrorState error={error} onRetry={retry} />
            : kind === 'courses' ? courseRows.length === 0 ? <EmptyState title={courses.data?.totalElements ? 'No courses match this page search' : 'No courses in the catalog'} description={courses.data?.totalElements ? 'Try another search or use the page controls.' : 'Courses will appear here when they are available in the campus catalog.'} />
              : <ResourceTable caption="Course catalog" columns={[
                { key: 'course', header: 'Course', render: (course: CourseResponse) => <div className="identity-cell"><span className="identity-avatar" aria-hidden="true">{course.courseCode.slice(0, 1)}</span><span><strong>{course.courseName}</strong><small>{course.courseCode}</small></span></div> },
                { key: 'department', header: 'Department', render: (course: CourseResponse) => departmentById.get(course.departmentId)?.name ?? 'Department unavailable' },
                { key: 'credits', header: 'Credits', render: (course: CourseResponse) => course.credits },
                { key: 'capacity', header: 'Capacity', mobileHidden: true, render: (course: CourseResponse) => course.capacity },
              ]} rows={courseRows} getRowKey={(course) => course.courseId} actions={(course) => <Button size="sm" variant="ghost" title="View course details" aria-label={`View ${course.courseCode} details`} onClick={() => setSelectedCourse(course)}><Eye size={16} aria-hidden="true" /></Button>} />
              : departmentRows.length === 0 ? <EmptyState title={departments.data?.length ? 'No departments match this search' : 'No departments available'} description={departments.data?.length ? 'Try another name or department code.' : 'Departments will appear here when they are available in the campus directory.'} />
                : <ResourceTable caption="Department directory" columns={[
                  { key: 'name', header: 'Department', render: (department: Department) => <div className="identity-cell"><span className="identity-avatar identity-avatar--violet" aria-hidden="true">{department.code.slice(0, 1)}</span><span><strong>{department.name}</strong><small>{department.code}</small></span></div> },
                ]} rows={departmentRows} getRowKey={(department) => department.departmentId} />}
        {kind === 'courses' && courses.data && <PaginationControls page={courses.data.page} size={courses.data.size} totalElements={courses.data.totalElements} totalPages={courses.data.totalPages} onPageChange={setPage} />}
      </Card>
      {selectedCourse && <RecordDetailsDialog open onOpenChange={(open) => { if (!open) setSelectedCourse(null) }} title={selectedCourse.courseName} description="Published course catalog details" fields={[
        { label: 'Course code', value: selectedCourse.courseCode },
        { label: 'Department', value: departmentById.get(selectedCourse.departmentId)?.name },
        { label: 'Credits', value: selectedCourse.credits },
        { label: 'Capacity', value: selectedCourse.capacity },
        { label: 'Status', value: selectedCourse.status },
      ]} />}
    </div>
  )
}
