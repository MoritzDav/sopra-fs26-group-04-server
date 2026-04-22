package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
public class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private CourseEnrollmentService courseEnrollmentService;

    @MockitoBean
    private OutlookService outlookService;

    /**
     * POST /courses/{courseId}/sessions
     */

    //Valid session start
    @Test
    void startSession_validRequest_returns201() throws Exception {

        // given
        Course course = new Course();
        course.setId(1L);

        Session session = new Session();
        session.setSessionId(1L);
        session.setTitle("Test Session");
        session.setActive(true);
        session.setMode(SessionMode.NORMAL);
        session.setCourse(course);
        session.setCreatedAt(LocalDateTime.now());
        session.setStart(LocalDateTime.now());

        SessionPostDTO dto = new SessionPostDTO();
        dto.setTitle("Test Session");

        given(sessionService.startSession(eq(1L), eq("teacher-token"), any(Session.class))).willReturn(session);

        // when
        MockHttpServletRequestBuilder request = post("/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "teacher-token")
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", is(1)))
                .andExpect(jsonPath("$.title", is("Test Session")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void startSession_invalidToken_returns401() throws Exception {
        // given
        SessionPostDTO dto = new SessionPostDTO();
        dto.setTitle("Test Session");

        given(sessionService.startSession(eq(1L), eq("invalid-token"), any(Session.class)))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when
        MockHttpServletRequestBuilder request = post("/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "invalid-token")
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startSession_notTeacher_returns403() throws Exception {
        // given
        SessionPostDTO dto = new SessionPostDTO();
        dto.setTitle("Test Session");

        given(sessionService.startSession(eq(1L), eq("student-token"), any(Session.class)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can start a session"));

        // when
        MockHttpServletRequestBuilder request = post("/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "student-token")
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void startSession_courseNotFound_returns404() throws Exception {
        // given
        SessionPostDTO dto = new SessionPostDTO();
        dto.setTitle("Test Session");

        given(sessionService.startSession(eq(99L), eq("teacher-token"), any(Session.class)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // when
        MockHttpServletRequestBuilder request = post("/courses/99/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "teacher-token")
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * PUT /courses/{courseId}/sessions/{sessionId}/end
     */

    @Test
    void endSession_validRequest_returns204() throws Exception {
        // given
        doNothing().when(sessionService).endSession(1L, "teacher-token");

        // when
        MockHttpServletRequestBuilder request = put("/courses/1/sessions/1/end")
                .header("Authorization", "teacher-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNoContent());
    }

    @Test
    void endSession_invalidToken_returns401() throws Exception {
        // given
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .when(sessionService).endSession(1L, "invalid-token");

        // when
        MockHttpServletRequestBuilder request = put("/courses/1/sessions/1/end")
                .header("Authorization", "invalid-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endSession_notTeacher_returns403() throws Exception {
        // given
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can end a session"))
                .when(sessionService).endSession(1L, "student-token");

        // when
        MockHttpServletRequestBuilder request = put("/courses/1/sessions/1/end")
                .header("Authorization", "student-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void endSession_sessionNotFound_returns404() throws Exception {
        // given
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"))
                .when(sessionService).endSession(99L, "teacher-token");

        // when
        MockHttpServletRequestBuilder request = put("/courses/1/sessions/99/end")
                .header("Authorization", "teacher-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }


    //Helper function to mock dto to JSON
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        }
        catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }

    }
}
