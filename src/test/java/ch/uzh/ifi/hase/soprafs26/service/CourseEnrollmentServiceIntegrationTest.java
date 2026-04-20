package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseEnrollmentRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CourseEnrollmentServiceIntegrationTest
 * 
 * Integration tests for course enrollment functionality using real H2 database.
 */
@WebAppConfiguration
@SpringBootTest
public class CourseEnrollmentServiceIntegrationTest {

	@Qualifier("userRepository")
	@Autowired
	private UserRepository userRepository;

	@Qualifier("courseRepository")
	@Autowired
	private CourseRepository courseRepository;

	@Qualifier("courseEnrollmentRepository")
	@Autowired
	private CourseEnrollmentRepository courseEnrollmentRepository;

	@Autowired
	private CourseEnrollmentService courseEnrollmentService;

	@BeforeEach
	public void setup() {
		courseEnrollmentRepository.deleteAll();
		courseRepository.deleteAll();
		userRepository.deleteAll();
	}

	private User createUser(String firstName, String lastName, String username, UserRole role) {
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setUsername(username);
		user.setPassword("password123");
		user.setRole(role);
		user.setStatus(UserStatus.ONLINE);
		user.setToken(UUID.randomUUID().toString());
		return user;
	}

	// ============ Join Course Tests ============

	@Test
	public void enrollStudentByCourseCode_validStudent_success() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		Course course = new Course();
		course.setTitle("Introduction to Java");
		course.setCourseCode("JAVA101");
		course.setTeacher(savedTeacher);
		Course savedCourse = courseRepository.save(course);

		User student = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent = userRepository.save(student);

		// when
		CourseEnrollment enrollment = courseEnrollmentService.enrollStudentByCourseCode(
				savedStudent.getId(), "JAVA101");

		// then
		assertNotNull(enrollment.getId());
		assertEquals(savedStudent.getId(), enrollment.getStudentId());
		assertEquals(savedCourse.getId(), enrollment.getCourseId());
		assertNotNull(enrollment.getJoinedDate());

		// Verify persisted in database
		List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseId(savedCourse.getId());
		assertEquals(1, enrollments.size());
		assertEquals(savedStudent.getId(), enrollments.get(0).getStudentId());
	}

	@Test
	public void enrollStudentByCourseCode_studentNotFound_throwsException() {
		// given, when & then
		assertThrows(ResponseStatusException.class,
				() -> courseEnrollmentService.enrollStudentByCourseCode(999L, "JAVA101"));
	}

	@Test
	public void enrollStudentByCourseCode_teacherCannotEnroll_throwsException() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> courseEnrollmentService.enrollStudentByCourseCode(savedTeacher.getId(), "JAVA101"));

		assertEquals("403 FORBIDDEN \"Only students can enroll in courses\"", exception.getMessage());
	}

	@Test
	public void enrollStudentByCourseCode_courseNotFound_throwsException() {
		// given
		User student = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent = userRepository.save(student);

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> courseEnrollmentService.enrollStudentByCourseCode(savedStudent.getId(), "INVALID"));

		assertEquals("404 NOT_FOUND \"Course code not found\"", exception.getMessage());
	}

	@Test
	public void enrollStudentByCourseCode_alreadyEnrolled_throwsException() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		Course course = new Course();
		course.setTitle("Introduction to Java");
		course.setCourseCode("JAVA101");
		course.setTeacher(savedTeacher);
		courseRepository.save(course);

		User student = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent = userRepository.save(student);

		// Enroll first time
		courseEnrollmentService.enrollStudentByCourseCode(savedStudent.getId(), "JAVA101");

		// when & then - Try to enroll again
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> courseEnrollmentService.enrollStudentByCourseCode(savedStudent.getId(), "JAVA101"));

		assertEquals("409 CONFLICT \"Student is already enrolled in this course\"", exception.getMessage());
	}

	@Test
	public void enrollMultipleStudentsInCourse() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		Course course = new Course();
		course.setTitle("Introduction to Java");
		course.setCourseCode("JAVA101");
		course.setTeacher(savedTeacher);
		Course savedCourse = courseRepository.save(course);

		User student1 = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent1 = userRepository.save(student1);

		User student2 = createUser("Jane", "Smith", "janesmith", UserRole.STUDENT);
		User savedStudent2 = userRepository.save(student2);

		// when
		CourseEnrollment enrollment1 = courseEnrollmentService.enrollStudentByCourseCode(
				savedStudent1.getId(), "JAVA101");
		CourseEnrollment enrollment2 = courseEnrollmentService.enrollStudentByCourseCode(
				savedStudent2.getId(), "JAVA101");

		// then
		assertNotNull(enrollment1.getId());
		assertNotNull(enrollment2.getId());
		assertNotEquals(enrollment1.getId(), enrollment2.getId());

		// Verify both enrolled
		List<CourseEnrollment> enrollments = courseEnrollmentService.getStudentsInCourse(savedCourse.getId());
		assertEquals(2, enrollments.size());
	}

	@Test
	public void enrollStudentInMultipleCourses() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		Course course1 = new Course();
		course1.setTitle("Introduction to Java");
		course1.setCourseCode("JAVA101");
		course1.setTeacher(savedTeacher);
		courseRepository.save(course1);

		Course course2 = new Course();
		course2.setTitle("Advanced Java");
		course2.setCourseCode("JAVA102");
		course2.setTeacher(savedTeacher);
		courseRepository.save(course2);

		User student = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent = userRepository.save(student);

		// when
		CourseEnrollment enrollment1 = courseEnrollmentService.enrollStudentByCourseCode(
				savedStudent.getId(), "JAVA101");
		CourseEnrollment enrollment2 = courseEnrollmentService.enrollStudentByCourseCode(
				savedStudent.getId(), "JAVA102");

		// then
		assertNotNull(enrollment1.getId());
		assertNotNull(enrollment2.getId());
		assertNotEquals(enrollment1.getCourseId(), enrollment2.getCourseId());

		// Verify both enrollments persisted
		List<CourseEnrollment> studentEnrollments = courseEnrollmentService.getStudentEnrollments(savedStudent.getId());
		assertEquals(2, studentEnrollments.size());
	}

	@Test
	public void getStudentsInCourse_multipleStudents() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		Course course = new Course();
		course.setTitle("Introduction to Java");
		course.setCourseCode("JAVA101");
		course.setTeacher(savedTeacher);
		Course savedCourse = courseRepository.save(course);

		User student1 = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent1 = userRepository.save(student1);

		User student2 = createUser("Jane", "Smith", "janesmith", UserRole.STUDENT);
		User savedStudent2 = userRepository.save(student2);

		// when
		courseEnrollmentService.enrollStudentByCourseCode(savedStudent1.getId(), "JAVA101");
		courseEnrollmentService.enrollStudentByCourseCode(savedStudent2.getId(), "JAVA101");

		// then
		List<CourseEnrollment> enrollments = courseEnrollmentService.getStudentsInCourse(savedCourse.getId());
		assertEquals(2, enrollments.size());

		List<Long> studentIds = enrollments.stream().map(CourseEnrollment::getStudentId).toList();
		assertTrue(studentIds.contains(savedStudent1.getId()));
		assertTrue(studentIds.contains(savedStudent2.getId()));
	}

	@Test
	public void getStudentEnrollments_multipleEnrollments() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		Course course1 = new Course();
		course1.setTitle("Introduction to Java");
		course1.setCourseCode("JAVA101");
		course1.setTeacher(savedTeacher);
		courseRepository.save(course1);

		Course course2 = new Course();
		course2.setTitle("Advanced Java");
		course2.setCourseCode("JAVA102");
		course2.setTeacher(savedTeacher);
		courseRepository.save(course2);

		User student = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent = userRepository.save(student);

		// when
		courseEnrollmentService.enrollStudentByCourseCode(savedStudent.getId(), "JAVA101");
		courseEnrollmentService.enrollStudentByCourseCode(savedStudent.getId(), "JAVA102");

		// then
		List<CourseEnrollment> enrollments = courseEnrollmentService.getStudentEnrollments(savedStudent.getId());
		assertEquals(2, enrollments.size());

		List<Long> courseIds = enrollments.stream().map(CourseEnrollment::getCourseId).toList();
		assertEquals(2, courseIds.stream().distinct().count()); // Both courses should be different
	}

	@Test
	public void joinedDate_isRecorded() {
		// given
		User teacher = createUser("Professor", "Smith", "prof_smith", UserRole.TEACHER);
		User savedTeacher = userRepository.save(teacher);

		Course course = new Course();
		course.setTitle("Introduction to Java");
		course.setCourseCode("JAVA101");
		course.setTeacher(savedTeacher);
		courseRepository.save(course);

		User student = createUser("John", "Doe", "johndoe", UserRole.STUDENT);
		User savedStudent = userRepository.save(student);

		// when
		CourseEnrollment enrollment = courseEnrollmentService.enrollStudentByCourseCode(
				savedStudent.getId(), "JAVA101");

		// then
		assertNotNull(enrollment.getJoinedDate());

		// Verify persisted with joined date
		CourseEnrollment retrieved = courseEnrollmentRepository.findById(enrollment.getId()).orElse(null);
		assertNotNull(retrieved);
		assertNotNull(retrieved.getJoinedDate());
	}

}

