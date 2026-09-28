import { Eye, GraduationCap } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { EmptyState } from '../../components/data-display/EmptyState'
import { ErrorState } from '../../components/data-display/ErrorState'
import { LoadingState } from '../../components/data-display/LoadingState'
import { ResourceTable } from '../../components/data-display/ResourceTable'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { DirectoryToolbar } from '../admin-shared/DirectoryToolbar'
import { RecordDetailsDialog } from '../admin-shared/RecordDetailsDialog'
import { StatusBadge } from '../admin-shared/StatusBadge'
import { departmentKeys, getDepartments } from '../departments/departments.api'
import { getStudents, studentKeys } from '../students/students.api'
import type { StudentResponse } from '../../types/api'

export function FacultyStudentsPage() {
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<StudentResponse | null>(null)
  const students = useQuery({ queryKey: studentKeys.all, queryFn: getStudents })
  const departments = useQuery({ queryKey: departmentKeys.all, queryFn: getDepartments })
  const departmentById = useMemo(() => new Map((departments.data ?? []).map((item) => [item.departmentId, item])), [departments.data])
  const filtered = useMemo(() => (students.data ?? []).filter((student) => {
    const department = departmentById.get(student.departmentId)
    return `${student.firstName} ${student.lastName ?? ''} ${student.enrollmentNumber} ${department?.name ?? ''} ${student.status}`.toLowerCase().includes(search.trim().toLowerCase())
  }), [students.data, departmentById, search])
  const columns = [
    { key: 'student', header: 'Student', render: (student: StudentResponse) => <div className="identity-cell"><span className="identity-avatar identity-avatar--teal" aria-hidden="true">{student.firstName.slice(0, 1).toUpperCase()}</span><span><strong>{student.firstName} {student.lastName}</strong><small>{student.enrollmentNumber}</small></span></div> },
    { key: 'department', header: 'Department', mobileHidden: true, render: (student: StudentResponse) => departmentById.get(student.departmentId)?.name ?? 'Department unavailable' },
    { key: 'admission', header: 'Admission year', mobileHidden: true, render: (student: StudentResponse) => student.admissionYear },
    { key: 'status', header: 'Status', render: (student: StudentResponse) => <StatusBadge status={student.status} /> },
  ]

  return (
    <div className="admin-page">
      <PageHeader eyebrow="Academic directory" title="Students" description="Read-only student information available to faculty accounts." />
      <Card className="directory-card">
        <DirectoryToolbar search={search} onSearch={setSearch} searchLabel="Search students" countLabel={`${filtered.length} of ${students.data?.length ?? 0} students`} />
        {students.isPending || departments.isPending ? <LoadingState label="Loading student directory" />
          : students.isError ? <ErrorState error={students.error} onRetry={() => void students.refetch()} />
            : departments.isError ? <ErrorState error={departments.error} onRetry={() => void departments.refetch()} />
              : filtered.length === 0 ? <EmptyState title={students.data.length === 0 ? 'No student profiles available' : 'No students match this search'} description={students.data.length === 0 ? 'Student profiles will appear here when they are available to your account.' : 'Try another student name, enrollment number or status.'} />
                : <ResourceTable caption="Read-only student directory" columns={columns} rows={filtered} getRowKey={(student) => student.studentId} actions={(student) => <div className="row-actions"><Button size="sm" variant="ghost" aria-label={`View ${student.firstName} ${student.lastName ?? ''}`} title="View student details" onClick={() => setSelected(student)}><Eye size={16} aria-hidden="true" /></Button></div>} />}
      </Card>
      <RecordDetailsDialog open={selected !== null} onOpenChange={(open) => { if (!open) setSelected(null) }} title={selected ? `${selected.firstName} ${selected.lastName ?? ''}`.trim() : 'Student details'} description="Student information available for academic work." fields={selected ? [
        { label: 'Enrollment number', value: selected.enrollmentNumber },
        { label: 'Department', value: departmentById.get(selected.departmentId)?.name },
        { label: 'Admission year', value: selected.admissionYear },
        { label: 'Status', value: selected.status },
        { label: 'Phone', value: selected.phone },
      ] : []} notice={<p className="detail-notice"><GraduationCap size={16} aria-hidden="true" /> Student records are read-only in the faculty workspace.</p>} />
    </div>
  )
}
