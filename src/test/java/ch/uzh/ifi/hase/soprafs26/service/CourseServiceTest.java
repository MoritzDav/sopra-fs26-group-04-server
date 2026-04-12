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

public class CourseServiceTest {
    
    @Mock
    private CourseRepository courseRepository;
 
    @Mock
    private UserRepository userRepository;
 
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
 
        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setToken("other-token");
 
        course = new Course();
        course.setId(10L);
        course.setTitle("Original Title");
        course.setDescription("Original Description");
        course.setPictureURL("http://original.com/pic.jpg");
        course.setTeacher(teacher);
        course.setCourseCode("ABC123");
    }


    /**
     * updateCourse
     */

    //Check whether updating works with a valid owner and while updating all the possible fields
    @Test
    public void updateCourse_validOwner_allFieldsUpdated(){
        
        //given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title");
        dto.setDescription("New Description");
        dto.setPictureURL("http://new.com/pic.jpg");
 
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);
 
        //when
        courseService.updateCourse(10L, "valid-token", dto);
 
        //then
        assertEquals("New Title", course.getTitle());
        assertEquals("New Description", course.getDescription());
        assertEquals("http://new.com/pic.jpg", course.getPictureURL());
        verify(courseRepository).save(course);
    }

    //Check whether updating only a part i.e. title works
    @Test
    public void updateCourse_partialUpdate_onlyTitleChanged(){
        
        //given only a title, no description and pictureURL
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("New Title");
 
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);
 
        //when
        courseService.updateCourse(10L, "valid-token", dto);
 
        //then
        assertEquals("New Title", course.getTitle());
        assertEquals("Original Description", course.getDescription());
        assertEquals("http://original.com/pic.jpg", course.getPictureURL());
    }

    //check whether updating-function with an empty DTO as input works
    @Test
    public void updateCourse_emptyDTO_nothingChanges(){
        
        //given no information in the dto
        CoursePutDTO dto = new CoursePutDTO();
 
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);
 
        //when
        courseService.updateCourse(10L, "valid-token", dto);
 
        //then
        assertEquals("Original Title", course.getTitle());
        assertEquals("Original Description", course.getDescription());
        assertEquals("http://original.com/pic.jpg", course.getPictureURL());
    }

    //Check whether updating with an invalid token throws not found
    @Test
    public void updateCourse_invalidToken_notFound(){

        //given
        CoursePutDTO dto = new CoursePutDTO();
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        //when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(10L, "invalid-token", dto));
        
        //then
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    //Check whether updating with an invalid courseId throws not found
    @Test
    public void updateCourse_invalidCourse_notFound(){
        //given
        CoursePutDTO dto = new CoursePutDTO();
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());


        //when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                courseService.updateCourse(99L, "valid-token", dto));
        
        //then
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    //Check whether updating as not the Owner throws a forbidden
    @Test
    public void updateCourse_notOwner_throwsForbidden(){
        //given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Test title");
        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherUser));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        //when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            courseService.updateCourse(10L, "other-token", dto));

        //then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }


    /**
     * Delete course
     */


    @Test
    public void deleteCourse_validOwner(){

        //given
        when(userRepository.findByToken("valid-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        //when
        courseService.deleteCourse(10L, "valid-token");

        // then
        verify(courseRepository).delete(course);
    }
    
    @Test
    public void deleteCourse_invalidToken_notfFound(){

        //given
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        //when
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            courseService.deleteCourse(10L, "invalid-token"));
            
        //then
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
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

}
