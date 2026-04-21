package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseEnrollmentRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CourseEnrollmentServiceTest {

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseEnrollmentService courseEnrollmentService;

    private User student;
    private User teacher;
    private Course course;
    private CourseEnrollment enrollment;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        student = new User();
        student.setId(1L);
        student.setRole(UserRole.STUDENT);

        teacher = new User();
        teacher.setId(2L);
        teacher.setRole(UserRole.TEACHER);

        course = new Course();
        course.setId(10L);
        course.setCourseCode("ABC123");
        course.setTitle("Math 101");
        course.setTeacher(teacher);

        enrollment = new CourseEnrollment();
        enrollment.setId(100L);
        enrollment.setStudentId(1L);
        enrollment.setCourseId(10L);
        enrollment.setJoinedDate(LocalDateTime.now());
    }


    /**
     * enrollStudentByCourseCode
     */

    @Test
    public void enrollStudent_validInput_success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findByCourseCode("ABC123")).thenReturn(course);
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());
        when(courseEnrollmentRepository.save(any())).thenReturn(enrollment);

        // when
        CourseEnrollment result = courseEnrollmentService.enrollStudentByCourseCode(1L, "ABC123");

        // then
        assertNotNull(result);
        assertEquals(1L, result.getStudentId());
        assertEquals(10L, result.getCourseId());
        verify(courseEnrollmentRepository).save(any());
    }

    @Test
    public void enrollStudent_studentNotFound_throwsNotFound() {
        // given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseEnrollmentService.enrollStudentByCourseCode(99L, "ABC123"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(courseEnrollmentRepository, never()).save(any());
    }

    @Test
    public void enrollStudent_userIsTeacher_throwsForbidden() {
        // given
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseEnrollmentService.enrollStudentByCourseCode(2L, "ABC123"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(courseEnrollmentRepository, never()).save(any());
    }

    @Test
    public void enrollStudent_courseCodeNotFound_throwsNotFound() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findByCourseCode("BADCODE")).thenReturn(null);

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseEnrollmentService.enrollStudentByCourseCode(1L, "BADCODE"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(courseEnrollmentRepository, never()).save(any());
    }

    @Test
    public void enrollStudent_alreadyEnrolled_throwsConflict() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findByCourseCode("ABC123")).thenReturn(course);
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrollment));

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseEnrollmentService.enrollStudentByCourseCode(1L, "ABC123"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(courseEnrollmentRepository, never()).save(any());
    }


    /**
     * getStudentsInCourse
     */

    @Test
    public void getStudentsInCourse_validCourse_returnsList() {
        // given
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.findByCourseId(10L)).thenReturn(List.of(enrollment));

        // when
        List<CourseEnrollment> result = courseEnrollmentService.getStudentsInCourse(10L);

        // then
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getStudentId());
    }

    @Test
    public void getStudentsInCourse_courseNotFound_throwsNotFound() {
        // given
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseEnrollmentService.getStudentsInCourse(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }


    /**
     * getStudentEnrollments
     */

    @Test
    public void getStudentEnrollments_validStudent_returnsList() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseEnrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));

        // when
        List<CourseEnrollment> result = courseEnrollmentService.getStudentEnrollments(1L);

        // then
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getCourseId());
    }

    @Test
    public void getStudentEnrollments_studentNotFound_throwsNotFound() {
        // given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseEnrollmentService.getStudentEnrollments(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}