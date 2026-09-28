import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { departmentKeys, getDepartments } from '../departments/departments.api'
import { courseKeys, getCourses } from '../courses/courses.api'
import type { CourseResponse } from '../../types/api'

export function StudentCatalogPage() {
  const [search, setSearch] = useState('')
  const courses = useQuery({ queryKey: courseKeys.all, queryFn: getCourses })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const departmentById = useMemo(() => new Map((departments.data ?? []).map((department) => [department.departmentId, department.name])), [departments.data])
  const filtered = useMemo(() => (courses.data ?? []).filter((course) => `${course.courseCode} ${course.courseName} ${departmentById.get(course.departmentId) ?? ''}`.toLowerCase().includes(search.trim().toLowerCase())), [courses.data, departmentById, search])
  return (
    <div className="admin-page">
      <PageHeader eyebrow="Campus catalog" title="Course catalog" description="Browse course descriptions published in the campus catalog. Your personal enrollments are not available through this view." />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search course catalog" countLabel={`${filtered.length} of ${courses.data?.length ?? 0} courses`} />
        {courses.isPending || departments.isPending ? <LoadingState label="Loading course catalog" />
          : courses.isError ? <ErrorState error={courses.error} onRetry={() => void courses.refetch()} />
            : departments.isError ? <ErrorState error={departments.error} onRetry={() => void departments.refetch()} />
              : filtered.length === 0 ? <EmptyState title={courses.data.length ? 'No courses match this search' : 'No courses available'} description={courses.data.length ? 'Try another course code, title or department.' : 'The campus catalog has no courses to show yet.'} />
                : <ResourceTable caption="Campus course catalog" columns={[
                  { key: 'course', header: 'Course', render: (course: CourseResponse) => <div className="identity-cell"><span className="identity-avatar" aria-hidden="true">{course.courseCode.slice(0, 1)}</span><span><strong>{course.courseName}</strong><small>{course.courseCode}</small></span></div> },
                  { key: 'department', header: 'Department', render: (course: CourseResponse) => departmentById.get(course.departmentId) ?? 'Department unavailable' },
                  { key: 'credits', header: 'Credits', mobileHidden: true, render: (course: CourseResponse) => course.credits },
                ]} rows={filtered} getRowKey={(course) => course.courseId} />}
      </Card>
    </div>
  )
}
