package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.BrowniePointEntryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseEnrollmentRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrowniePointServiceTest {

    @Mock private BrowniePointEntryRepository browniePointEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;

    @InjectMocks
    private BrowniePointService browniePointService;

    private User teacher;
    private User student;
    private User outsider;
    private Course course;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        teacher = new User();
        teacher.setId(1L);
        teacher.setToken("teacher-token");
        teacher.setRole(UserRole.TEACHER);

        student = new User();
        student.setId(2L);
        student.setToken("student-token");
        student.setRole(UserRole.STUDENT);

        outsider = new User();
        outsider.setId(3L);
        outsider.setToken("outsider-token");
        outsider.setRole(UserRole.STUDENT);

        course = new Course();
        course.setId(10L);
        course.setTeacher(teacher);
    }

    @Test
    void getLeaderboard_teacherSuccess() {
        User leaderboardStudent = new User();
        leaderboardStudent.setId(2L);
        leaderboardStudent.setUsername("student1");
        leaderboardStudent.setFirstName("Student");
        leaderboardStudent.setLastName("One");

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());
        List<Object[]> leaderboardRows = new ArrayList<>();
        leaderboardRows.add(new Object[]{leaderboardStudent, 20L});
        when(browniePointEntryRepository.findLeaderboardByCourseId(10L)).thenReturn(leaderboardRows);

        assertEquals(20L, browniePointService.getLeaderboard(10L, "teacher-token").get(0).getTotalPoints());
    }

    @Test
    void getLeaderboard_enrolledStudentSuccess() {
        User leaderboardStudent = new User();
        leaderboardStudent.setId(2L);
        leaderboardStudent.setUsername("student1");
        leaderboardStudent.setFirstName("Student");
        leaderboardStudent.setLastName("One");

        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(2L, 10L)).thenReturn(Optional.of(new CourseEnrollment()));
        List<Object[]> leaderboardRows = new ArrayList<>();
        leaderboardRows.add(new Object[]{leaderboardStudent, 20L});
        when(browniePointEntryRepository.findLeaderboardByCourseId(10L)).thenReturn(leaderboardRows);

        assertEquals(20L, browniePointService.getLeaderboard(10L, "student-token").get(0).getTotalPoints());
    }

    @Test
    void getLeaderboard_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                browniePointService.getLeaderboard(10L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void getLeaderboard_notPartOfCourse_throwsForbidden() {
        when(userRepository.findByToken("outsider-token")).thenReturn(Optional.of(outsider));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(3L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                browniePointService.getLeaderboard(10L, "outsider-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void getLeaderboard_courseNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                browniePointService.getLeaderboard(10L, "teacher-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
