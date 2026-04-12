package ch.uzh.ifi.hase.soprafs26.controller;
 
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
