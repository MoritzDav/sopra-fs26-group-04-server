package ch.uzh.ifi.hase.soprafs26.controller;
 
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CourseGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePutDTO;
import ch.uzh.ifi.hase.soprafs26.service.CourseEnrollmentService;
import ch.uzh.ifi.hase.soprafs26.service.CourseService;
import ch.uzh.ifi.hase.soprafs26.service.OutlookService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
public class CourseControllerTest {
   
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private CourseEnrollmentService courseEnrollmentService; 

    @MockitoBean
    private OutlookService outlookService;

    /**
     * POST /courses
     */
    //checks course creation works
    @Test
    public void createCourse_validRequest_returns201WithBody() throws Exception {
        // given
        CoursePostDTO dto = new CoursePostDTO();
        dto.setTitle("Math 101");
        dto.setDescription("Introduction to Math");
        dto.setTeacherId(1L);

        User teacher = new User();
        teacher.setId(1L);

        Course created = new Course();
        created.setId(10L);
        created.setTitle("Math 101");
        created.setDescription("Introduction to Math");
        created.setCourseCode("XYZ999");
        created.setTeacher(teacher);

        given(courseService.newCourse(any(Course.class), eq(1L))).willReturn(created);

        // when
        MockHttpServletRequestBuilder request = post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.title", is("Math 101")))
                .andExpect(jsonPath("$.courseCode", is("XYZ999")));
    }
    //course has no teacher throws error
    @Test
    public void createCourse_teacherNotFound_returns404() throws Exception {
        // given
        CoursePostDTO dto = new CoursePostDTO();
        dto.setTitle("Math 101");
        dto.setTeacherId(99L);

        given(courseService.newCourse(any(Course.class), eq(99L)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User/teacher not found"));

        // when
        MockHttpServletRequestBuilder request = post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }
    //checks courses can only be created by teachers
    @Test
    public void createCourse_userIsNotTeacher_returns403() throws Exception {
        // given
        CoursePostDTO dto = new CoursePostDTO();
        dto.setTitle("Math 101");
        dto.setTeacherId(2L);

        given(courseService.newCourse(any(Course.class), eq(2L)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers are allowed to create a new course"));

        // when
        MockHttpServletRequestBuilder request = post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }


    /**
     * PUT /courses/{courseId}
     */

    //Checks if a valid request returns 204
    @Test
    public void updateCourse_validRequest_returns204() throws Exception{
        
        //given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Test Title");

        doNothing().when(courseService).updateCourse(eq(1L), eq("valid-token"), any(CoursePutDTO.class));


        //when
        MockHttpServletRequestBuilder request = put("/courses/1")
            .header("Authorization", "valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(dto));

        //then
        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    //Checks if an invalid token leads to a 401
    @Test
    public void updateCourse_invalidToken_returns401() throws Exception{

        //given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Test Title");

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
            .when(courseService).updateCourse(eq(1L), eq("invalid-token"), any(CoursePutDTO.class));


        //when
        MockHttpServletRequestBuilder request = put("/courses/1")
            .header("Authorization", "invalid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(dto));

        //then
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized());
    }

    //Checks if being not an owner of the course returns a 403
    @Test
    public void updateCourse_notOwner_returns403() throws Exception{
        
        //given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Test Title");

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of the course"))
            .when(courseService).updateCourse(eq(1L), eq("other-token"), any(CoursePutDTO.class));

        //when
        MockHttpServletRequestBuilder request = put("/courses/1")
            .header("Authorization", "other-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(dto));

        //then
        mockMvc.perform(request)
            .andExpect(status().isForbidden());

    }

    //Checks if course not found returns a 404
    @Test
    public void updateCourse_courseNotFound_returns404() throws Exception{

        
        //given
        CoursePutDTO dto = new CoursePutDTO();
        dto.setTitle("Test Title");

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"))
            .when(courseService).updateCourse(eq(99L), eq("valid-token"), any(CoursePutDTO.class));

        //when
        MockHttpServletRequestBuilder request = put("/courses/99")
            .header("Authorization", "valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(dto));

        //then
        mockMvc.perform(request)
            .andExpect(status().isNotFound());

    }

    //Checks if empty body works as well
    @Test
    public void updateCourse_emptyBody_returns204() throws Exception {
        
        // given
        CoursePutDTO dto = new CoursePutDTO();
 
        doNothing().when(courseService).updateCourse(eq(1L), eq("valid-token"), any(CoursePutDTO.class));
 
        // when
        MockHttpServletRequestBuilder request = put("/courses/1")
                .header("Authorization", "valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));
 
        // then
        mockMvc.perform(request)
                .andExpect(status().isNoContent());
    }


    /**
     * DELETE /courses/{courseId}
     */

    //Checks if a valid request returns a 204
    @Test
    public void deleteCourse_validRequest_returns204() throws Exception {
        
        // given
        doNothing().when(courseService).deleteCourse(eq(1L), eq("valid-token"));
 
        // when
        MockHttpServletRequestBuilder request = delete("/courses/1")
                .header("Authorization", "valid-token");
 
        // then
        mockMvc.perform(request)
                .andExpect(status().isNoContent());
    }

    //Checks if an invalid token returns a 401
    @Test
    public void deleteCourse_invalidToken_returns401() throws Exception {
        
        // given
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .when(courseService).deleteCourse(eq(1L), eq("invalid-token"));
 
        // when
        MockHttpServletRequestBuilder request = delete("/courses/1")
                .header("Authorization", "invalid-token");
 
        // then
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    //Checks if someone who is not the owner wants to change anything => 403
    @Test
    public void deleteCourse_notOwner_returns403() throws Exception {
        // given
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of the course"))
                .when(courseService).deleteCourse(eq(1L), eq("other-token"));
 
        // when
        MockHttpServletRequestBuilder request = delete("/courses/1")
                .header("Authorization", "other-token");
 
        // then
        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    //Checks if an invalid courseId leads to a 404
    @Test
    public void deleteCourse_courseNotFound_returns404() throws Exception {
        
        // given
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"))
                .when(courseService).deleteCourse(eq(99L), eq("valid-token"));
 
        // when
        MockHttpServletRequestBuilder request = delete("/courses/99")
                .header("Authorization", "valid-token");
 
        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * GET /courses/{courseId}
     */

    @Test
    public void getCourse_validId_returns200() throws Exception {
        // given
        User teacher = new User();
        teacher.setId(1L);

        Course course = new Course();
        course.setId(10L);
        course.setTitle("Math 101");
        course.setDescription("Intro to Math");
        course.setCourseCode("XYZ999");
        course.setTeacher(teacher);

        given(courseService.getCourseById(10L)).willReturn(course);

        // when
        MockHttpServletRequestBuilder request = get("/courses/10");

        // then
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.title", is("Math 101")))
                .andExpect(jsonPath("$.courseCode", is("XYZ999")));
    }

    @Test
    public void getCourse_invalidId_returns404() throws Exception {
        // given
        given(courseService.getCourseById(99L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // when
        MockHttpServletRequestBuilder request = get("/courses/99");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }


    /**
     * POST /courses/{courseCode}/enroll
     */

    @Test
    public void enrollStudent_validRequest_returns201() throws Exception {
        // given
        ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment enrollment =
                new ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment();
        enrollment.setId(100L);
        enrollment.setStudentId(1L);
        enrollment.setCourseId(10L);
        enrollment.setJoinedDate(java.time.LocalDateTime.now());

        given(courseEnrollmentService.enrollStudentByCourseCode(1L, "ABC123")).willReturn(enrollment);

        // when
        MockHttpServletRequestBuilder request = post("/courses/ABC123/enroll")
                .param("studentId", "1");

        // then
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId", is(1)))
                .andExpect(jsonPath("$.courseId", is(10)));
    }

    @Test
    public void enrollStudent_studentNotFound_returns404() throws Exception {
        // given
        given(courseEnrollmentService.enrollStudentByCourseCode(99L, "ABC123"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // when
        MockHttpServletRequestBuilder request = post("/courses/ABC123/enroll")
                .param("studentId", "99");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void enrollStudent_courseNotFound_returns404() throws Exception {
        // given
        given(courseEnrollmentService.enrollStudentByCourseCode(1L, "BADCODE"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course code not found"));

        // when
        MockHttpServletRequestBuilder request = post("/courses/BADCODE/enroll")
                .param("studentId", "1");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void enrollStudent_alreadyEnrolled_returns409() throws Exception {
        // given
        given(courseEnrollmentService.enrollStudentByCourseCode(1L, "ABC123"))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Student is already enrolled in this course"));

        // when
        MockHttpServletRequestBuilder request = post("/courses/ABC123/enroll")
                .param("studentId", "1");

        // then
        mockMvc.perform(request)
                .andExpect(status().isConflict());
    }


    /**
     * GET /courses/{courseCode}/students
     */

    @Test
    public void getStudentsInCourse_validCode_returns200() throws Exception {
        // given
        User teacher = new User();
        teacher.setId(1L);

        Course course = new Course();
        course.setId(10L);
        course.setCourseCode("ABC123");
        course.setTitle("Math 101");
        course.setTeacher(teacher);

        ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment enrollment =
                new ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment();
        enrollment.setId(100L);
        enrollment.setStudentId(1L);
        enrollment.setCourseId(10L);
        enrollment.setJoinedDate(java.time.LocalDateTime.now());

        given(courseService.getCourseByCourseCode("ABC123")).willReturn(course);
        given(courseEnrollmentService.getStudentsInCourse(10L)).willReturn(java.util.List.of(enrollment));

        // when
        MockHttpServletRequestBuilder request = get("/courses/ABC123/students");

        // then
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId", is(1)))
                .andExpect(jsonPath("$[0].courseId", is(10)));
    }

    @Test
    public void getStudentsInCourse_courseNotFound_returns404() throws Exception {
        // given
        given(courseService.getCourseByCourseCode("BADCODE"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course code not found"));

        // when
        MockHttpServletRequestBuilder request = get("/courses/BADCODE/students");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }


    //Helper function to convert dto into a JSON-string

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
