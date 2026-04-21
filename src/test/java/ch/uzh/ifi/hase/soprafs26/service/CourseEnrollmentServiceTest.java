package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseEnrollmentRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CourseEnrollmentServiceTest
 *
 * Unit tests for course enrollment functionality (join course).
 * Tests verify student enrollment in courses with Mockito mocking.
 */
public class CourseEnrollmentServiceTest {

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseEnrollmentService courseEnrollmentService;

    private User testStudent;
    private User testTeacher;
    private Course testCourse;
    private CourseEnrollment testEnrollment;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testStudent = new User();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setUsername("johndoe");
        testStudent.setPassword("password123");
        testStudent.setRole(UserRole.STUDENT);

        testTeacher = new User();
        testTeacher.setId(2L);
        testTeacher.setFirstName("Prof");
        testTeacher.setLastName("Smith");
        testTeacher.setUsername("prof_smith");
        testTeacher.setPassword("password123");
        testTeacher.setRole(UserRole.TEACHER);

        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Introduction to Java");
        testCourse.setCourseCode("JAVA101");
        testCourse.setTeacher(testTeacher);

        testEnrollment = new CourseEnrollment();
        testEnrollment.setId(1L);
        testEnrollment.setStudentId(testStudent.getId());
        testEnrollment.setCourseId(testCourse.getId());
        testEnrollment.setJoinedDate(LocalDateTime.now());
    }

    // ============ Join Course Tests ============

    @Test
    public void enrollStudentByCourseCode_validStudent_success() {
        // given
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        Mockito.when(courseRepository.findByCourseCode("JAVA101")).thenReturn(testCourse);
        Mockito.when(courseEnrollmentRepository.findByStudentIdAndCourseId(1L, 1L)).thenReturn(Optional.empty());
        Mockito.when(courseEnrollmentRepository.save(Mockito.any())).thenReturn(testEnrollment);

        // when
        CourseEnrollment enrollment = courseEnrollmentService.enrollStudentByCourseCode(1L, "JAVA101");

        // then
        Mockito.verify(courseEnrollmentRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(courseEnrollmentRepository, Mockito.times(1)).flush();

        assertNotNull(enrollment.getId());
        assertEquals(1L, enrollment.getStudentId());
        assertEquals(1L, enrollment.getCourseId());
        assertNotNull(enrollment.getJoinedDate());
    }

    @Test
    public void enrollStudentByCourseCode_studentNotFound_throwsException() {
        // given
        Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> courseEnrollmentService.enrollStudentByCourseCode(999L, "JAVA101"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Student not found"));
    }

    @Test
    public void enrollStudentByCourseCode_teacherCannotEnroll_throwsException() {
        // given
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(testTeacher));

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> courseEnrollmentService.enrollStudentByCourseCode(2L, "JAVA101"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Only students can enroll"));

        Mockito.verify(courseEnrollmentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void enrollStudentByCourseCode_courseNotFound_throwsException() {
        // given
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        Mockito.when(courseRepository.findByCourseCode("INVALID")).thenReturn(null);

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> courseEnrollmentService.enrollStudentByCourseCode(1L, "INVALID"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Course code not found"));

        Mockito.verify(courseEnrollmentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void enrollStudentByCourseCode_alreadyEnrolled_throwsException() {
        // given
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        Mockito.when(courseRepository.findByCourseCode("JAVA101")).thenReturn(testCourse);
        Mockito.when(courseEnrollmentRepository.findByStudentIdAndCourseId(1L, 1L))
                .thenReturn(Optional.of(testEnrollment));

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> courseEnrollmentService.enrollStudentByCourseCode(1L, "JAVA101"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already enrolled"));

        Mockito.verify(courseEnrollmentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void enrollStudentByCourseCode_setsJoinedDateToNow() {
        // given
        LocalDateTime beforeEnrollment = LocalDateTime.now();

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        Mockito.when(courseRepository.findByCourseCode("JAVA101")).thenReturn(testCourse);
        Mockito.when(courseEnrollmentRepository.findByStudentIdAndCourseId(1L, 1L)).thenReturn(Optional.empty());
        Mockito.when(courseEnrollmentRepository.save(Mockito.any())).thenAnswer(invocation -> {
            CourseEnrollment enrollmentArg = invocation.getArgument(0);
            return enrollmentArg;
        });

        // when
        CourseEnrollment enrollment = courseEnrollmentService.enrollStudentByCourseCode(1L, "JAVA101");

        // then
        assertNotNull(enrollment.getJoinedDate());
        assertFalse(enrollment.getJoinedDate().isBefore(beforeEnrollment));
    }

    @Test
    public void enrollMultipleStudents_multipleCourses() {
        // given
        User student2 = new User();
        student2.setId(3L);
        student2.setRole(UserRole.STUDENT);

        Course course2 = new Course();
        course2.setId(2L);
        course2.setCourseCode("JAVA102");

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        Mockito.when(courseRepository.findByCourseCode("JAVA101")).thenReturn(testCourse);
        Mockito.when(courseEnrollmentRepository.findByStudentIdAndCourseId(1L, 1L)).thenReturn(Optional.empty());

        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(student2));
        Mockito.when(courseRepository.findByCourseCode("JAVA102")).thenReturn(course2);
        Mockito.when(courseEnrollmentRepository.findByStudentIdAndCourseId(3L, 2L)).thenReturn(Optional.empty());

        Mockito.when(courseEnrollmentRepository.save(Mockito.any())).thenAnswer(invocation -> {
            CourseEnrollment enrollment = invocation.getArgument(0);
            if (enrollment.getStudentId() == 1L) {
                enrollment.setId(1L);
            } else if (enrollment.getStudentId() == 3L) {
                enrollment.setId(2L);
            }
            return enrollment;
        });

        // when
        CourseEnrollment result1 = courseEnrollmentService.enrollStudentByCourseCode(1L, "JAVA101");
        CourseEnrollment result2 = courseEnrollmentService.enrollStudentByCourseCode(3L, "JAVA102");

        // then
        assertNotEquals(result1.getId(), result2.getId());
        assertEquals(1L, result1.getStudentId());
        assertEquals(3L, result2.getStudentId());
    }

    // ============ Get Students in Course Tests ============

    @Test
    public void getStudentsInCourse_validCourseId_success() {
        // given
        List<CourseEnrollment> enrollments = Arrays.asList(testEnrollment);
        Mockito.when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        Mockito.when(courseEnrollmentRepository.findByCourseId(1L)).thenReturn(enrollments);

        // when
        List<CourseEnrollment> result = courseEnrollmentService.getStudentsInCourse(1L);

        // then
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getStudentId());
    }

    @Test
    public void getStudentsInCourse_courseNotFound_throwsException() {
        // given
        Mockito.when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> courseEnrollmentService.getStudentsInCourse(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Course not found"));
    }

    @Test
    public void getStudentsInCourse_emptyEnrollments() {
        // given
        Mockito.when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        Mockito.when(courseEnrollmentRepository.findByCourseId(1L)).thenReturn(Arrays.asList());

        // when
        List<CourseEnrollment> result = courseEnrollmentService.getStudentsInCourse(1L);

        // then
        assertTrue(result.isEmpty());
    }

    // ============ Get Student Enrollments Tests ============

    @Test
    public void getStudentEnrollments_validStudent_returnsList() {
        // given
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        Mockito.when(courseEnrollmentRepository.findByStudentId(1L)).thenReturn(Arrays.asList(testEnrollment));

        // when
        List<CourseEnrollment> result = courseEnrollmentService.getStudentEnrollments(1L);

        // then
        assertEquals(1, result.size());
        assertEquals(testCourse.getId(), result.get(0).getCourseId());
    }

    @Test
    public void getStudentEnrollments_studentNotFound_throwsException() {
        // given
        Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> courseEnrollmentService.getStudentEnrollments(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Student not found"));
    }
}