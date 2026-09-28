package com.campuscore.backend.service;

import com.campuscore.backend.dto.StudentDashboardResponse;
import com.campuscore.backend.entity.AttendanceRecord;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentDashboardService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final GpaCalculationService gpaCalculationService;

    public StudentDashboardService(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            GpaCalculationService gpaCalculationService) {

        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.gpaCalculationService = gpaCalculationService;
    }

    public StudentDashboardResponse getStudentDashboard(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudentStudentId(studentId);

        List<StudentDashboardResponse.CourseSummary> courses =
                new ArrayList<>();

        List<String> alerts = new ArrayList<>();

        BigDecimal totalGradePoints = BigDecimal.ZERO;
        int totalCredits = 0;

        for (Enrollment enrollment : enrollments) {

            List<AttendanceRecord> attendanceRecords =
                    attendanceRecordRepository
                            .findByEnrollmentEnrollmentId(
                                    enrollment.getEnrollmentId()
                            );

            double attendancePercentage =
                    calculateAttendancePercentage(attendanceRecords);

            courses.add(
                    new StudentDashboardResponse.CourseSummary(
                            enrollment.getCourse().getCourseId(),
                            enrollment.getCourse().getCourseCode(),
                            enrollment.getCourse().getCourseName(),
                            enrollment.getCourse().getCredits(),
                            attendancePercentage
                    )
            );

            if (attendancePercentage < 75.0) {

                alerts.add(
                        "Attendance below 75% in "
                                + enrollment.getCourse().getCourseCode()
                );
            }

            /*
             * Calculate GPA for this course.
             */
            try {

                GpaCalculationService.CourseGpaResult result =
                        gpaCalculationService.calculateCourseGpa(
                                enrollment.getEnrollmentId(),
                                "DEFAULT_10_POINT"
                        );

                /*
                 * Only completed courses should contribute
                 * to the final GPA.
                 */
                if (result.isComplete()) {

                    int credits =
                            enrollment.getCourse().getCredits();

                    BigDecimal gradePoint =
                            result.getGradePoint();

                    totalGradePoints =
                            totalGradePoints.add(
                                    gradePoint.multiply(
                                            BigDecimal.valueOf(credits)
                                    )
                            );

                    totalCredits += credits;
                }

            } catch (RuntimeException ignored) {

                /*
                 * If a course does not yet have enough
                 * assessment data, it is skipped from
                 * the final GPA calculation.
                 */
            }
        }

        double gpa = 0.0;

        if (totalCredits > 0) {

            gpa = totalGradePoints
                    .divide(
                            BigDecimal.valueOf(totalCredits),
                            2,
                            java.math.RoundingMode.HALF_UP
                    )
                    .doubleValue();
        }

        return new StudentDashboardResponse(
                student.getStudentId(),
                buildStudentName(student),
                student.getEnrollmentNumber(),
                gpa,
                courses,
                alerts
        );
    }

    private double calculateAttendancePercentage(
            List<AttendanceRecord> records) {

        if (records.isEmpty()) {
            return 0.0;
        }

        long presentCount = records.stream()
                .filter(record ->
                        record.getStatus()
                                == AttendanceRecord.Status.PRESENT)
                .count();

        return (presentCount * 100.0) / records.size();
    }

    private String buildStudentName(Student student) {

        if (student.getLastName() == null
                || student.getLastName().isBlank()) {

            return student.getFirstName();
        }

        return student.getFirstName()
                + " "
                + student.getLastName();
    }
}