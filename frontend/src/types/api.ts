export type Role = 'ADMIN' | 'FACULTY' | 'STUDENT'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  role: Role
}

export interface MeResponse {
  userId: number
  username: string
  email: string
  role: Role
  active: boolean
}

export interface StudentSelfResponse {
  studentId: number
  enrollmentNumber: string
  firstName: string
  lastName: string | null
  departmentName: string
  admissionYear: number
  status: 'ACTIVE' | 'INACTIVE' | 'GRADUATED'
}

export interface FacultySelfResponse {
  employeeNumber: string
  firstName: string
  lastName: string | null
  departmentName: string
  status: 'ACTIVE' | 'INACTIVE'
}

export interface UserRequest {
  username: string
  email: string
  password: string
  role: Role
}

export interface UserResponse {
  userId: number
  username: string
  email: string
  role: Role
  active: boolean
}

export interface StudentRequest {
  userId: number
  departmentId: number
  enrollmentNumber: string
  firstName: string
  lastName?: string | null
  dateOfBirth?: string | null
  phone?: string | null
  admissionYear: number
}

export interface StudentResponse {
  studentId: number
  userId: number
  departmentId: number
  enrollmentNumber: string
  firstName: string
  lastName: string | null
  dateOfBirth: string | null
  phone: string | null
  admissionYear: number
  status: 'ACTIVE' | 'INACTIVE' | 'GRADUATED'
}

export interface FacultyResponse {
  facultyId: number
  userId: number
  departmentId: number
  employeeNumber: string
  firstName: string
  lastName: string | null
  phone: string | null
  status: 'ACTIVE' | 'INACTIVE'
}

export interface Department {
  departmentId: number
  name: string
  code: string
}

export interface CourseRequest {
  departmentId: number
  courseCode: string
  courseName: string
  credits: number
  capacity: number
}

export interface CourseResponse {
  courseId: number
  departmentId: number
  courseCode: string
  courseName: string
  credits: number
  capacity: number
  status: 'ACTIVE' | 'INACTIVE'
}

export interface EnrollmentResponse {
  enrollmentId: number
  studentId: number
  courseId: number
  semester: string
  academicYear: string
  enrollmentDate: string
  status: 'ENROLLED' | 'DROPPED' | 'COMPLETED'
}

export type AssessmentType =
  | 'QUIZ'
  | 'ASSIGNMENT'
  | 'MIDTERM'
  | 'FINAL'
  | 'PROJECT'

export interface AssessmentResponse {
  assessmentId: number
  courseId: number
  name: string
  assessmentType: AssessmentType
  maxMarks: number
  weight: number
  assessmentDate: string | null
}

export interface MarkResponse {
  markId: number
  enrollmentId: number
  assessmentId: number
  marksObtained: string
  enteredAt: string
}

export interface AttendanceSessionResponse {
  attendanceSessionId: number
  courseId: number
  facultyId: number
  sessionDate: string
  topic: string | null
}

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE'

export interface AttendanceRecordResponse {
  attendanceRecordId: number
  attendanceSessionId: number
  enrollmentId: number
  status: AttendanceStatus
}

export interface GradingPolicy {
  gradingPolicyId: number
  name: string
  minPercentage: number
  maxPercentage: number
  grade: string
  gradePoint: number
}

export interface GpaResult {
  enrollmentId: number
  courseId: number
  courseCode: string
  courseName: string
  assessedWeight: number
  currentPercentage: number
  grade: string
  gradePoint: number
  complete: boolean
}

export interface StudentDashboardResponse {
  studentId: number
  studentName: string
  enrollmentNumber: string
  gpa: number
  courses: StudentCourseSummary[]
  alerts: string[]
}

export interface StudentCourseSummary {
  courseId: number
  courseCode: string
  courseName: string
  credits: number
  attendancePercentage: number
}

export interface AuditLogResponse {
  auditLogId: number
  userId: number | null
  action: string
  entityName: string
  entityId: number | null
  description: string | null
  createdAt: string
}

export interface GradingPolicyRequest {
  name: string
  minPercentage: number
  maxPercentage: number
  grade: string
  gradePoint: number
}

export interface DepartmentRequest {
  name: string
  code: string
}

export interface ApiValidationPayload {
  status?: number
  error?: string
  errors?: Record<string, string>
}
