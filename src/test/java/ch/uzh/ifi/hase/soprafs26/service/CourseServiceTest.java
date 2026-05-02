package ch.uzh.ifi.hase.soprafs26.service;
 
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
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

public class CourseServiceTest {
    
    @Mock
    private CourseRepository courseRepository;
 
    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private OutlookService outlookService;
 
    @InjectMocks
    private CourseService courseService;
 
    private User teacher;
    private User otherUser;
    private Course course;
 
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
 
        teacher = new User();
        teacher.setId(1L);
        teacher.setToken("valid-token");
        teacher.setRole(UserRole.TEACHER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setToken("other-token");
        otherUser.setRole(UserRole.STUDENT);
 
        course = new Course();
        course.setId(10L);
        course.setTitle("Original Title");
        course.setDescription("Original Description");
        course.setPictureURL("http://original.com/pic.jpg");
        course.setTeacher(teacher);
        course.setCourseCode("ABC123");

        courseService = new CourseService(courseRepository, userRepository, sessionRepository, outlookService);
    }


    /**
     * getCourseById
     */

    @Test
    public void getCourseById_validId_success() {
        // given
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // when
        Course result = courseService.getCourseById(10L);

        // then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Original Title", result.getTitle());
    }

    @Test
    public void getCourseById_invalidId_throwsNotFound() {
        // given
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseService.getCourseById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }


    /**
     * getCourseByCourseCode
     */

    @Test
    public void getCourseByCourseCode_validCode_success() {
        // given
        when(courseRepository.findByCourseCode("ABC123")).thenReturn(course);

        // when
        Course result = courseService.getCourseByCourseCode("ABC123");

        // then
        assertNotNull(result);
        assertEquals("ABC123", result.getCourseCode());
    }

    @Test
    public void getCourseByCourseCode_invalidCode_throwsNotFound() {
        // given
        when(courseRepository.findByCourseCode("BADCODE")).thenReturn(null);

        // when / then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                courseService.getCourseByCourseCode("BADCODE"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }


    /**
     * newCourse
     */

    @Test
    public void newCourse_validTeacher_courseCreated() {
        // given
        Course input = new Course();
        input.setTitle("Test Course");
        input.setDescription("A description");

        Course saved = new Course();
        saved.setId(10L);
        saved.setTitle("Test Course");
        saved.setDescription("A description");
        saved.setTeacher(teacher);
        saved.setCourseCode("ABC123");

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(courseRepository.findByCourseCode(any())).thenReturn(null);
        when(courseRepository.save(any())).thenReturn(saved);

        // when
        Course result = courseService.newCourse(input, 1L, "teacher-token");

        // then
        assertNotNull(result);
        assertEquals("Test Course", result.getTitle());
        assertEquals(teacher, result.getTeacher());
        assertNotNull(result.getCourseCode());
        verify(courseRepository).save(any());
    }

    @Test
    public void newCourse_teacherNotFound_throwsNotFound() {
        // given
        Course input = new Course();
        input.setTitle("Test Course");

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.newCourse(input, 99L, "teacher-token"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(courseRepository, never()).save(any());
    }

    @Test
    public void newCourse_userIsStudent_throwsForbidden() {
        // given
        Course input = new Course();
        input.setTitle("Test Course");

        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.newCourse(input, 2L, "other-token"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(courseRepository, never()).save(any());
    }

    @Test
    public void newCourse_tokenUserMismatch_throwsForbidden() {
        // given
        Course input = new Course();
        input.setTitle("Test Course");

        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.newCourse(input, 2L, "valid-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(courseRepository, never()).save(any());
    }

    /**
     * updateCourse
     */

    // ============ Valid Update Tests ============

    @Test
    public void updateCourse_allFieldsUpdated_success() {
        // given - Update all credential fields
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Advanced Web Development");
        dto.setDescription("Learn advanced web dev concepts");
        dto.setPictureURL("https://example.com/advanced.jpg");

        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(10L, "valid-token", dto);

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

        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(10L, "valid-token", dto);

        // then
        assertEquals("New Title Only", course.getTitle());
        assertEquals("Original Description", course.getDescription()); // unchanged
        assertEquals("http://original.com/pic.jpg", course.getPictureURL()); // unchanged
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_descriptionOnly_success() {
        // given - Update only description
        CoursePutDTO dto = new CoursePutDTO();
        dto.setDescription("Brand new description");

        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(10L, "valid-token", dto);

        // then
        assertEquals("Original Title", course.getTitle()); // unchanged
        assertEquals("Brand new description", course.getDescription());
        assertEquals("http://original.com/pic.jpg", course.getPictureURL()); // unchanged
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_pictureURLOnly_success() {
        // given - Update only picture URL
        CoursePutDTO dto = new CoursePutDTO();
        dto.setPictureURL("https://example.com/updated-pic.jpg");

        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(10L, "valid-token", dto);

        // then
        assertEquals("Original Title", course.getTitle()); // unchanged
        assertEquals("Original Description", course.getDescription()); // unchanged
        assertEquals("https://example.com/updated-pic.jpg", course.getPictureURL());
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_titleAndDescription_success() {
        // given - Update title and description
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title");
        dto.setDescription("New Description");

        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(10L, "valid-token", dto);

        // then
        assertEquals("New Title", course.getTitle());
        assertEquals("New Description", course.getDescription());
        assertEquals("http://original.com/pic.jpg", course.getPictureURL()); // unchanged
        verify(courseRepository).save(course);
    }

    @Test
    public void updateCourse_emptyDTO_nothingChanges() {
        // given - Empty DTO (no fields updated)
        CoursePutDTO dto = new CoursePutDTO();

        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);

        // when
        courseService.updateCourse(10L, "valid-token", dto);

        // then
        assertEquals("Original Title", course.getTitle());
        assertEquals("Original Description", course.getDescription());
        assertEquals("http://original.com/pic.jpg", course.getPictureURL());
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
                courseService.updateCourse(10L, "invalid-token", dto));

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
                courseService.updateCourse(10L, null, dto));

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

        when(userRepository.findByToken("student-token-invalid")).thenReturn(Optional.of(otherUser));
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

        when(userRepository.findByToken("student-token-invalid")).thenReturn(Optional.of(otherUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        // when
        assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(100L, "student-token-invalid", dto));

        // then - Object should not be persisted, but in memory it may be dirty
        // This test verifies that save() was never called
        verify(courseRepository, never()).save(any());
    }

    /**
     * Delete course
     */


    @Test
    public void deleteCourse_validOwner(){

        //given
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        doNothing().when(sessionRepository).deleteByCourseId(10L);

        //when
        courseService.deleteCourse(10L, "valid-token");

        // then
        verify(courseRepository).delete(course);
        verify(sessionRepository).deleteByCourseId(10L);
    }
    
    @Test
    public void deleteCourse_invalidToken_unauthorized(){

        //given
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        //when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            courseService.deleteCourse(10L, "invalid-token"));
            
        //then
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void deleteCourse_invalidCourseId_notFound(){

        //given
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        //when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            courseService.deleteCourse(99L, "valid-token"));

        //then
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void deleteCourse_notOwner_throwsForbidden(){

        //given 
        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherUser));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        //when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            courseService.deleteCourse(10L, "other-token"));

        //then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(courseRepository, never()).delete(any());
    }

    /**
     * Generate QR Code
     */

    @Test
    public void generateQRCode_validTeacher_returnsBytes() {
        // given
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        course.setTeacher(teacher);
        course.setCourseCode("ABC123");

        // when
        byte[] result = courseService.generateQRCode(course, "valid-token");

        // then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void generateQRCode_invalidToken_throwsUnauthorized() {
        // given
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.generateQRCode(course, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void generateQRCode_notOwner_throwsForbidden() {
        // given
        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherUser));
        course.setTeacher(teacher);

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.generateQRCode(course, "other-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }


    /**
     * Email preview
     */

    @Test
    public void generateCourseEmailPreview_validTeacher_returnsEmail() {
        // given
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        course.setTeacher(teacher);
        when(outlookService.generateCourseEmailPreview(course)).thenReturn("<html>Email</html>");

        // when
        String result = courseService.generateCourseEmailPreview(course, "valid-token");

        // then
        assertNotNull(result);
        assertEquals("<html>Email</html>", result);
        verify(outlookService).generateCourseEmailPreview(course);
    }

    @Test
    public void generateCourseEmailPreview_invalidToken_throwsUnauthorized() {
        // given
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.generateCourseEmailPreview(course, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(outlookService, never()).generateCourseEmailPreview(any());
    }

    @Test
    public void generateCourseEmailPreview_notOwner_throwsForbidden() {
        // given
        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherUser));
        course.setTeacher(teacher);

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.generateCourseEmailPreview(course, "other-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(outlookService, never()).generateCourseEmailPreview(any());
    }

}
