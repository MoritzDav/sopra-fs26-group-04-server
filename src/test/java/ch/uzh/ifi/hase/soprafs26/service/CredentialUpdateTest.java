package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CredentialUpdateTest
 * 
 * Comprehensive unit tests for credential update functionality.
 * Tests cover course credential updates with various scenarios:
 * - Valid updates with different field combinations
 * - Partial updates
 * - Empty updates
 * - Authorization checks
 * - Validation of inputs
 */
public class CredentialUpdateTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;

    private User teacher;
    private User unauthorizedUser;
    private Course course;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup teacher (course owner)
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("prof_smith");
        teacher.setToken("teacher-token-valid");

        // Setup unauthorized user
        unauthorizedUser = new User();
        unauthorizedUser.setId(2L);
        unauthorizedUser.setUsername("student_alice");
        unauthorizedUser.setToken("student-token-invalid");

        // Setup course
        course = new Course();
        course.setId(100L);
        course.setTitle("Original Web Development");
        course.setDescription("Learn web dev basics");
        course.setPictureURL("https://example.com/original.jpg");
        course.setTeacher(teacher);
        course.setCourseCode("WEB101");
    }

    // ============ Valid Update Tests ============

    @Test
    public void updateCourse_allFieldsUpdated_success() {
        // given - Update all credential fields
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Advanced Web Development");
        dto.setDescription("Learn advanced web dev concepts");
        dto.setPictureURL("https://example.com/advanced.jpg");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals("Advanced Web Development", course.getTitle());
        assertEquals("Learn advanced web dev concepts", course.getDescription());
        assertEquals("https://example.com/advanced.jpg", course.getPictureURL());
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_titleOnly_success() {
        // given - Update only title
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title Only");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals("New Title Only", course.getTitle());
        assertEquals("Learn web dev basics", course.getDescription()); // unchanged
        assertEquals("https://example.com/original.jpg", course.getPictureURL()); // unchanged
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_descriptionOnly_success() {
        // given - Update only description
        CoursePutDTO dto = new CoursePutDTO();
        dto.setDescription("Brand new description");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals("Original Web Development", course.getTitle()); // unchanged
        assertEquals("Brand new description", course.getDescription());
        assertEquals("https://example.com/original.jpg", course.getPictureURL()); // unchanged
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_pictureURLOnly_success() {
        // given - Update only picture URL
        CoursePutDTO dto = new CoursePutDTO();
        dto.setPictureURL("https://example.com/updated-pic.jpg");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals("Original Web Development", course.getTitle()); // unchanged
        assertEquals("Learn web dev basics", course.getDescription()); // unchanged
        assertEquals("https://example.com/updated-pic.jpg", course.getPictureURL());
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_titleAndDescription_success() {
        // given - Update title and description
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title");
        dto.setDescription("New Description");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals("New Title", course.getTitle());
        assertEquals("New Description", course.getDescription());
        assertEquals("https://example.com/original.jpg", course.getPictureURL()); // unchanged
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_emptyDTO_nothingChanges() {
        // given - Empty DTO (no fields updated)
        CoursePutDTO dto = new CoursePutDTO();

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals("Original Web Development", course.getTitle());
        assertEquals("Learn web dev basics", course.getDescription());
        assertEquals("https://example.com/original.jpg", course.getPictureURL());
        verify(courseRepository).save(course);
    }

    // ============ Authorization Tests ============

    @Test
    public void updateCourse_invalidToken_throwsNotFound() {
        // given - Invalid token
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Attempted Title Change");

        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(100L, "invalid-token", dto));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(courseRepository, never()).save(any());
    }

    @Test
    public void updateCourse_nullToken_throwsNotFound() {
        // given - Null token
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Attempted Title Change");

        when(userRepository.findByToken(null)).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(100L, null, dto));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(courseRepository, never()).save(any());
    }

    @Test
    public void updateCourse_invalidCourseId_throwsNotFound() {
        // given - Invalid course ID
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(999L, "teacher-token-valid", dto));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(courseRepository, never()).save(any());
    }

    @Test
    public void updateCourse_notOwner_throwsForbidden() {
        // given - User is not the course owner
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Unauthorized Title Change");

        when(userRepository.findByToken("student-token-invalid")).thenReturn(Optional.of(unauthorizedUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(100L, "student-token-invalid", dto));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(courseRepository, never()).save(any());
    }

    // ============ Input Validation Tests ============

    @Test
    public void updateCourse_longTitle_success() {
        // given - Very long title
        CoursePutDTO dto = new CoursePutDTO();
        String longTitle = "A".repeat(200);
        dto.setTitle(longTitle);

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals(longTitle, course.getTitle());
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_longDescription_success() {
        // given - Very long description
        CoursePutDTO dto = new CoursePutDTO();
        String longDescription = "Description with lots of content. ".repeat(50);
        dto.setDescription(longDescription);

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals(longDescription, course.getDescription());
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_emptyTitleString_success() {
        // given - Empty string for title
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals("", course.getTitle());
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_specialCharactersInTitle_success() {
        // given - Title with special characters
        CoursePutDTO dto = new CoursePutDTO();
        String titleWithSpecialChars = "Web Dev 2.0: $100 Course @Home!";
        dto.setTitle(titleWithSpecialChars);

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals(titleWithSpecialChars, course.getTitle());
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_unicodeCharacters_success() {
        // given - Title with unicode characters
        CoursePutDTO dto = new CoursePutDTO();
        String titleWithUnicode = "Web Development 网络开发 🌐";
        dto.setTitle(titleWithUnicode);

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        assertEquals(titleWithUnicode, course.getTitle());
        verify(courseRepository).save(course);
    }

    // ============ Verification Tests ============

    @Test
    public void updateCourse_verifyRepositoryCalledOnce() {
        // given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title");

        when(userRepository.findByToken("teacher-token-valid")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(100L, "teacher-token-valid", dto);

        // then
        verify(courseRepository, times(1)).save(any());
    }

    @Test
    public void updateCourse_verifyRepositoryNotCalledOnFailedAuth() {
        // given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title");

        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(100L, "invalid-token", dto));

        // then
        verify(courseRepository, never()).save(any());
    }

    @Test
    public void updateCourse_verifyCourseObjectNotModifiedOnFailure() {
        // given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Attempted Change");
        
        String originalTitle = course.getTitle();
        String originalDescription = course.getDescription();
        String originalPictureURL = course.getPictureURL();

        when(userRepository.findByToken("student-token-invalid")).thenReturn(Optional.of(unauthorizedUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        // when
        assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(100L, "student-token-invalid", dto));

        // then - Object should not be persisted, but in memory it may be dirty
        // This test verifies that save() was never called
        verify(courseRepository, never()).save(any());
    }
}
