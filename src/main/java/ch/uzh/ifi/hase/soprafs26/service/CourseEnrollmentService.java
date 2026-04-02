package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseEnrollmentRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseEnrollmentService {

    private final Logger log = LoggerFactory.getLogger(CourseEnrollmentService.class);

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseEnrollmentService(
            @Qualifier("courseEnrollmentRepository") CourseEnrollmentRepository courseEnrollmentRepository,
            @Qualifier("courseRepository") CourseRepository courseRepository,
            @Qualifier("userRepository") UserRepository userRepository) {
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    /**
     * Enroll a student in a course using course code.
     * Validates:
     * - Student exists and has role STUDENT
     * - Course code exists
     * - Student is not already enrolled
     */
    public CourseEnrollment enrollStudentByCourseCode(Long studentId, String courseCode) {
        // Validate student exists
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // Validate student role
        if (student.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can enroll in courses");
        }

        // Validate course exists by code
        Course course = courseRepository.findByCourseCode(courseCode);
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course code not found");
        }

        // Check if already enrolled
        if (isStudentEnrolled(studentId, course.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already enrolled in this course");
        }

        // Create enrollment
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(course.getId());
        enrollment.setJoinedDate(LocalDateTime.now());

        enrollment = courseEnrollmentRepository.save(enrollment);
        courseEnrollmentRepository.flush();

        log.debug("Student {} enrolled in course {} ({})", studentId, course.getId(), courseCode);
        return enrollment;
    }

    /**
     * Get all students enrolled in a course
     */
    public List<CourseEnrollment> getStudentsInCourse(Long courseId) {
        // Validate course exists
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        return courseEnrollmentRepository.findByCourseId(courseId);
    }

    /**
     * Get all courses a student is enrolled in
     */
    public List<CourseEnrollment> getStudentEnrollments(Long studentId) {
        // Validate student exists
        userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        return courseEnrollmentRepository.findByStudentId(studentId);
    }

    /**
     * Check if a student is enrolled in a course
     */
    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return courseEnrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).isPresent();
    }
}
