package com.campuscore.backend.service;

import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.FacultyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceSessionService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;

    public AttendanceSessionService(
            AttendanceSessionRepository attendanceSessionRepository,
            CourseRepository courseRepository,
            FacultyRepository facultyRepository) {

        this.attendanceSessionRepository = attendanceSessionRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
    }

    public List<AttendanceSession> getAllSessions() {
        return attendanceSessionRepository.findAll();
    }

    public AttendanceSession getSessionById(Long id) {
        return attendanceSessionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Attendance session not found"));
    }

    public List<AttendanceSession> getSessionsByCourse(Long courseId) {
        return attendanceSessionRepository.findByCourseCourseId(courseId);
    }

    public List<AttendanceSession> getSessionsByFaculty(Long facultyId) {
        return attendanceSessionRepository.findByFacultyFacultyId(facultyId);
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
}