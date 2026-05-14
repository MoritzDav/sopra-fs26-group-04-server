package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.BrowniePointsPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderBoardEntryGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.BrowniePointService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrowniePointController.class)
public class BrowniePointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrowniePointService browniePointService;

    // ── GET /courses/{courseId}/leaderboard ──────────────────────────────────

    @Test
    void getLeaderboard_validRequest_returns200() throws Exception {
        LeaderBoardEntryGetDTO entry = new LeaderBoardEntryGetDTO();
        entry.setUserId(2L);
        entry.setUsername("student1");
        entry.setFirstName("Student");
        entry.setLastName("One");
        entry.setTotalPoints(15L);

        given(browniePointService.getLeaderboard(eq(1L), eq("teacher-token"))).willReturn(List.of(entry));

        MockHttpServletRequestBuilder request = get("/courses/1/leaderboard")
                .header("Authorization", "teacher-token");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId", is(2)))
                .andExpect(jsonPath("$[0].username", is("student1")))
                .andExpect(jsonPath("$[0].totalPoints", is(15)));
    }

    @Test
    void getLeaderboard_invalidToken_returns401() throws Exception {
        given(browniePointService.getLeaderboard(eq(1L), eq("invalid-token")))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        MockHttpServletRequestBuilder request = get("/courses/1/leaderboard")
                .header("Authorization", "invalid-token");

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLeaderboard_notEnrolled_returns403() throws Exception {
        given(browniePointService.getLeaderboard(eq(1L), eq("other-token")))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this course"));

        MockHttpServletRequestBuilder request = get("/courses/1/leaderboard")
                .header("Authorization", "other-token");

        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void getLeaderboard_courseNotFound_returns404() throws Exception {
        given(browniePointService.getLeaderboard(eq(99L), eq("teacher-token")))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        MockHttpServletRequestBuilder request = get("/courses/99/leaderboard")
                .header("Authorization", "teacher-token");

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    // ── POST /courses/{courseId}/sessions/{sessionId}/browniepoints ──────────

    @Test
    void awardBrowniePoints_validRequest_returns201() throws Exception {
        BrowniePointsPostDTO dto = new BrowniePointsPostDTO();
        dto.setStudentId(2L);
        dto.setPoints(5);

        doNothing().when(browniePointService).awardBrowniePoints(eq(1L), eq(2L), eq(10L), eq(5), eq("teacher-token"));

        MockHttpServletRequestBuilder request = post("/courses/1/sessions/10/browniepoints")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "teacher-token")
                .content(new ObjectMapper().writeValueAsString(dto));

        mockMvc.perform(request)
                .andExpect(status().isCreated());
    }

    @Test
    void awardBrowniePoints_invalidToken_returns401() throws Exception {
        BrowniePointsPostDTO dto = new BrowniePointsPostDTO();
        dto.setStudentId(2L);
        dto.setPoints(5);

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .when(browniePointService).awardBrowniePoints(eq(1L), eq(2L), eq(10L), eq(5), eq("invalid-token"));

        MockHttpServletRequestBuilder request = post("/courses/1/sessions/10/browniepoints")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "invalid-token")
                .content(new ObjectMapper().writeValueAsString(dto));

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void awardBrowniePoints_notATeacher_returns403() throws Exception {
        BrowniePointsPostDTO dto = new BrowniePointsPostDTO();
        dto.setStudentId(2L);
        dto.setPoints(5);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can award points"))
                .when(browniePointService).awardBrowniePoints(eq(1L), eq(2L), eq(10L), eq(5), eq("student-token"));

        MockHttpServletRequestBuilder request = post("/courses/1/sessions/10/browniepoints")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "student-token")
                .content(new ObjectMapper().writeValueAsString(dto));

        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void awardBrowniePoints_studentNotFound_returns404() throws Exception {
        BrowniePointsPostDTO dto = new BrowniePointsPostDTO();
        dto.setStudentId(99L);
        dto.setPoints(5);

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"))
                .when(browniePointService).awardBrowniePoints(eq(1L), eq(99L), eq(10L), eq(5), eq("teacher-token"));

        MockHttpServletRequestBuilder request = post("/courses/1/sessions/10/browniepoints")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "teacher-token")
                .content(new ObjectMapper().writeValueAsString(dto));

        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }
}
