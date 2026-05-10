package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderBoardEntryGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.BrowniePointService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrowniePointController.class)
public class BrowniePointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrowniePointService browniePointService;

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
}
