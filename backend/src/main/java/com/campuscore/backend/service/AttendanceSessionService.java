package com.campuscore.backend.service;

import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.dto.AttendanceSessionRequest;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.FacultyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceSessionService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final AccessScope accessScope;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public AttendanceSessionService(
            AttendanceSessionRepository attendanceSessionRepository,
            CourseRepository courseRepository,
            FacultyRepository facultyRepository) {

        this(attendanceSessionRepository, courseRepository, facultyRepository, null, null);
    }

    public AttendanceSessionService(
            AttendanceSessionRepository attendanceSessionRepository,
            CourseRepository courseRepository,
            FacultyRepository facultyRepository,
            AccessScope accessScope) {

        this(attendanceSessionRepository, courseRepository, facultyRepository, accessScope, null);
    }

        @Autowired
    public AttendanceSessionService(
            AttendanceSessionRepository attendanceSessionRepository,
            CourseRepository courseRepository,
            FacultyRepository facultyRepository,
            AccessScope accessScope,
            AttendanceRecordRepository attendanceRecordRepository) {

        this.attendanceSessionRepository = attendanceSessionRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.accessScope = accessScope;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    public List<AttendanceSession> getAllSessions() {
        requireAdminForGlobalListing();
        return attendanceSessionRepository.findAll();
    }

    public Page<AttendanceSession> getSessionsPage(Pageable pageable) {
        requireAdminForGlobalListing();
        return attendanceSessionRepository.findAll(pageable);
    }

    public AttendanceSession getSessionById(Long id) {
        AttendanceSession session = attendanceSessionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Attendance session not found"));
        requireCourseAccess(session.getCourse().getCourseId());
        return session;
    }

    public List<AttendanceSession> getSessionsByCourse(Long courseId) {
        requireCourseAccess(courseId);
        return attendanceSessionRepository.findByCourseCourseId(courseId);
    }

    public Page<AttendanceSession> getSessionsByCoursePage(Long courseId, Pageable pageable) {
        requireCourseAccess(courseId);
        return attendanceSessionRepository.findByCourseCourseId(courseId, pageable);
    }

    public List<AttendanceSession> getSessionsByFaculty(Long facultyId) {
        // A faculty member may only list their own sessions; an administrator
        // may list anyone's.
        if (!accessScopeIsUsable() || !accessScope.isAdmin()) {
            Long currentFacultyId = currentFacultyIdOrThrow();
            if (!currentFacultyId.equals(facultyId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You cannot list another faculty member's attendance sessions");
            }
        }
        return attendanceSessionRepository.findByFacultyFacultyId(facultyId);
    }

    public Page<AttendanceSession> getSessionsByFacultyPage(Long facultyId, Pageable pageable) {
        return attendanceSessionRepository.findByFacultyFacultyId(facultyId, pageable);
    }

    public AttendanceSession createSession(
            Long courseId,
            Long facultyId,
            LocalDate sessionDate,
            String topic) {

        if (attendanceSessionRepository
                .existsByCourseCourseIdAndSessionDate(
                        courseId,
                        sessionDate)) {

            throw new RuntimeException(
                    "Attendance session already exists for this course on this date"
            );
        }

        requireCourseAccess(courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new RuntimeException("Course not found"));

        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() ->
                        new RuntimeException("Faculty not found"));

        AttendanceSession session = new AttendanceSession();

        session.setCourse(course);
        session.setFaculty(faculty);
        session.setSessionDate(sessionDate);
        session.setTopic(topic);

        return attendanceSessionRepository.save(session);
    }

    /**
     * Update a session's date and topic.
     *
     * <p>Course and faculty are deliberately not editable. Attendance records are
     * filed against the session, so moving a session to a different offering or
     * lecturer would file existing attendance under the wrong class.
     *
     * <p>The date stays editable even once attendance exists: correcting a
     * mistyped date is the common case and does not change what was recorded.
     */
    public AttendanceSession updateSession(Long id, AttendanceSessionRequest request) {
        AttendanceSession session = attendanceSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance session not found"));

        requireCourseAccess(session.getCourse().getCourseId());

        session.setSessionDate(request.getSessionDate());
        session.setTopic(request.getTopic());

        return attendanceSessionRepository.save(session);
    }

    /**
     * Delete a session that carries no attendance records.
     *
     * <p>An attendance record is the child of a session and the foreign key is
     * ON DELETE RESTRICT, so deleting a session that has attendance would fail at
     * the database with a constraint violation. Blocking it here turns that into a
     * message naming the recorded attendance. Sessions have no status column, so
     * there is no archive state to fall back on.
     */
    public void deleteSession(Long id) {
        AttendanceSession session = attendanceSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance session not found"));

        requireCourseAccess(session.getCourse().getCourseId());

        if (dependentAttendanceCount(id) > 0) {
            throw new ResourceConflictException(
                    "Attendance session cannot be deleted while attendance records exist for it");
        }

        attendanceSessionRepository.delete(session);
    }

    /**
     * Number of attendance records filed against a session.
     *
     * <p>Fails closed: with the repository unwired the dependency cannot be
     * checked, so the guard must refuse rather than assume deletion is safe.
     */
    private long dependentAttendanceCount(Long sessionId) {
        if (attendanceRecordRepository == null) {
            throw new ResourceConflictException(
                    "Attendance dependencies cannot be verified, so this session was left unchanged");
        }
        return Optional.ofNullable(
                attendanceRecordRepository.countByAttendanceSessionAttendanceSessionId(sessionId))
                .orElse(0L);
    }

    private void requireCourseAccess(Long courseId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        accessScope.requireCourseAccess(courseId);
    }

    private boolean accessScopeIsUsable() {
        return accessScope != null;
    }

    private Long currentFacultyIdOrThrow() {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        return accessScope.currentFacultyIdOrThrow();
    }

    /**
     * Guards the institution-wide listings.
     *
     * These getters are not scope filtered, so they are reserved for
     * administrators. Requiring the role here as well as in the request matcher
     * means a future route that calls one of them cannot silently expose every
     * record in the institution to a faculty member or a student.
     */
    private void requireAdminForGlobalListing() {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        if (!accessScope.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This listing is restricted to administrators");
        }
    }

}
