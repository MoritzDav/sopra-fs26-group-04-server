package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.PersonalWhiteboard;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.SessionFile;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionBoardDeselectDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionBoardSelectDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionCollaborationDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WhiteboardStateDTO;
import ch.uzh.ifi.hase.soprafs26.service.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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


    @Test
    void toggleCollaboration_enable_returns204() throws Exception {
        SessionCollaborationDTO dto = new SessionCollaborationDTO();
        dto.setCollaborationActive(true);

        doNothing().when(sessionService).toggleCollaboration(1L, "teacher-token", true);

        MockHttpServletRequestBuilder request = put("/sessions/1/collaboration")
                .header("Authorization", "teacher-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        mockMvc.perform(request)
                .andExpect(status().isNoContent());
    }

    @Test
    void toggleCollaboration_invalidToken_returns401() throws Exception {
        SessionCollaborationDTO dto = new SessionCollaborationDTO();
        dto.setCollaborationActive(true);

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .when(sessionService).toggleCollaboration(1L, "invalid-token", true);

        MockHttpServletRequestBuilder request = put("/sessions/1/collaboration")
                .header("Authorization", "invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void toggleCollaboration_notTeacher_returns403() throws Exception {
        SessionCollaborationDTO dto = new SessionCollaborationDTO();
        dto.setCollaborationActive(false);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can do this"))
                .when(sessionService).toggleCollaboration(1L, "student-token", false);

        MockHttpServletRequestBuilder request = put("/sessions/1/collaboration")
                .header("Authorization", "student-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void summarizeSessionFile_success_returnsPdfAttachment() throws Exception {
        byte[] pdfBytes = "fake-pdf".getBytes(StandardCharsets.UTF_8);
        given(sessionService.summarizeSessionFileToPdf(1L, 2L, "teacher-token")).willReturn(pdfBytes);

        MockHttpServletRequestBuilder request = post("/sessions/1/files/2/summarize")
                .header("Authorization", "teacher-token");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"summary-2.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void summarizeSessionFile_invalidToken_returns401() throws Exception {
        given(sessionService.summarizeSessionFileToPdf(1L, 2L, "invalid-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        MockHttpServletRequestBuilder request = post("/sessions/1/files/2/summarize")
                .header("Authorization", "invalid-token");

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    /**
     * GET /courses/{courseId}/sessions
     */

    @Test
    void getSessionsByCourse_validRequest_returns200() throws Exception {
        Course course = new Course();
        course.setId(1L);

        Session session = new Session();
        session.setSessionId(10L);
        session.setTitle("Lecture 1");
        session.setActive(false);
        session.setMode(SessionMode.NORMAL);
        session.setCourse(course);
        session.setCreatedAt(LocalDateTime.now());

        given(sessionService.getSessionsByCourse(eq(1L), eq("teacher-token"))).willReturn(List.of(session));

        mockMvc.perform(get("/courses/1/sessions").header("Authorization", "teacher-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sessionId", is(10)))
                .andExpect(jsonPath("$[0].title", is("Lecture 1")));
    }

    @Test
    void getSessionsByCourse_invalidToken_returns401() throws Exception {
        given(sessionService.getSessionsByCourse(eq(1L), eq("invalid-token")))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        mockMvc.perform(get("/courses/1/sessions").header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * POST /courses/{courseId}/sessions/{sessionId}/join
     */

    @Test
    void joinSession_validRequest_returns201() throws Exception {
        PersonalWhiteboard whiteboard = new PersonalWhiteboard();
        whiteboard.setWhiteboardId(5L);

        given(sessionService.joinSession(eq(1L), eq(10L), eq("student-token"))).willReturn(whiteboard);

        mockMvc.perform(post("/courses/1/sessions/10/join").header("Authorization", "student-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.whiteboardId", is(5)));
    }

    @Test
    void joinSession_invalidToken_returns401() throws Exception {
        given(sessionService.joinSession(eq(1L), eq(10L), eq("invalid-token")))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        mockMvc.perform(post("/courses/1/sessions/10/join").header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void joinSession_sessionNotFound_returns404() throws Exception {
        given(sessionService.joinSession(eq(1L), eq(99L), eq("student-token")))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        mockMvc.perform(post("/courses/1/sessions/99/join").header("Authorization", "student-token"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /courses/{courseId}/sessions/{sessionId}/whiteboard
     */

    @Test
    void getWhiteboardState_validRequest_returns200() throws Exception {
        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        dto.setCanvasSnapshot("snapshot-data");

        given(sessionService.getWhiteboardState(eq(1L))).willReturn(dto);

        mockMvc.perform(get("/courses/1/sessions/1/whiteboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canvasSnapshot", is("snapshot-data")));
    }

    /**
     * PUT /courses/{courseId}/sessions/{sessionId}/whiteboard
     */

    @Test
    void saveWhiteboardState_validRequest_returns204() throws Exception {
        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        dto.setCanvasSnapshot("snapshot-data");

        doNothing().when(sessionService).saveWhiteboardState(eq(1L), eq("teacher-token"), eq("snapshot-data"));

        mockMvc.perform(put("/courses/1/sessions/1/whiteboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "teacher-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void saveWhiteboardState_invalidToken_returns401() throws Exception {
        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        dto.setCanvasSnapshot("snapshot-data");

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .when(sessionService).saveWhiteboardState(eq(1L), eq("invalid-token"), any());

        mockMvc.perform(put("/courses/1/sessions/1/whiteboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "invalid-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * PUT /courses/{courseId}/sessions/{sessionId}/personal-whiteboard
     */

    @Test
    void savePersonalWhiteboardState_validRequest_returns204() throws Exception {
        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        dto.setCanvasSnapshot("my-snapshot");

        doNothing().when(sessionService).savePersonalWhiteboardState(eq(1L), eq(10L), eq("student-token"), eq("my-snapshot"));

        mockMvc.perform(put("/courses/1/sessions/10/personal-whiteboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "student-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void savePersonalWhiteboardState_invalidToken_returns401() throws Exception {
        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        dto.setCanvasSnapshot("my-snapshot");

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .when(sessionService).savePersonalWhiteboardState(eq(1L), eq(10L), eq("invalid-token"), any());

        mockMvc.perform(put("/courses/1/sessions/10/personal-whiteboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "invalid-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * GET /courses/{courseId}/sessions/{sessionId}/students/{studentId}/whiteboard
     */

    @Test
    void getStudentWhiteboardState_validRequest_returns200() throws Exception {
        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        dto.setCanvasSnapshot("student-snapshot");

        given(sessionService.getStudentWhiteboardState(eq(1L), eq(2L))).willReturn(dto);

        mockMvc.perform(get("/courses/1/sessions/1/students/2/whiteboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canvasSnapshot", is("student-snapshot")));
    }

    /**
     * PUT /courses/{courseId}/sessions/{sessionId}/selected-board
     */

    @Test
    void selectStudentBoard_validRequest_returns204() throws Exception {
        SessionBoardSelectDTO dto = new SessionBoardSelectDTO();
        dto.setStudentId(2L);

        doNothing().when(sessionService).selectStudentBoard(eq(1L), eq(10L), eq("teacher-token"), eq(2L));

        mockMvc.perform(put("/courses/1/sessions/10/selected-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "teacher-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void selectStudentBoard_notTeacher_returns403() throws Exception {
        SessionBoardSelectDTO dto = new SessionBoardSelectDTO();
        dto.setStudentId(2L);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can select a board"))
                .when(sessionService).selectStudentBoard(eq(1L), eq(10L), eq("student-token"), eq(2L));

        mockMvc.perform(put("/courses/1/sessions/10/selected-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "student-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    /**
     * PUT /courses/{courseId}/sessions/{sessionId}/deselect-board
     */

    @Test
    void deselectStudentBoard_validRequest_returns204() throws Exception {
        SessionBoardDeselectDTO dto = new SessionBoardDeselectDTO();
        dto.setStudentId(2L);
        dto.setCanvasSnapshot("final-snapshot");

        doNothing().when(sessionService).deselectStudentBoard(eq(1L), eq(10L), eq("teacher-token"), eq(2L), eq("final-snapshot"));

        mockMvc.perform(put("/courses/1/sessions/10/deselect-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "teacher-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deselectStudentBoard_notTeacher_returns403() throws Exception {
        SessionBoardDeselectDTO dto = new SessionBoardDeselectDTO();
        dto.setStudentId(2L);
        dto.setCanvasSnapshot("final-snapshot");

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can deselect a board"))
                .when(sessionService).deselectStudentBoard(eq(1L), eq(10L), eq("student-token"), eq(2L), any());

        mockMvc.perform(put("/courses/1/sessions/10/deselect-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "student-token")
                        .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    /**
     * GET /sessions/{sessionId}/files
     */

    @Test
    void getSessionFiles_validRequest_returns200() throws Exception {
        Session session = new Session();
        session.setSessionId(1L);

        SessionFile file = new SessionFile();
        file.setId(7L);
        file.setFileName("notes.pdf");
        file.setFileType("application/pdf");
        file.setData(new byte[0]);
        file.setUploadedAt(LocalDateTime.now());
        file.setSession(session);

        given(sessionService.getSessionFiles(eq(1L), eq("teacher-token"))).willReturn(List.of(file));

        mockMvc.perform(get("/sessions/1/files").header("Authorization", "teacher-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(7)))
                .andExpect(jsonPath("$[0].fileName", is("notes.pdf")));
    }

    @Test
    void getSessionFiles_invalidToken_returns401() throws Exception {
        given(sessionService.getSessionFiles(eq(1L), eq("invalid-token")))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        mockMvc.perform(get("/sessions/1/files").header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * POST /sessions/{sessionId}/files
     */

    @Test
    void uploadSessionFile_validRequest_returns201() throws Exception {
        Session session = new Session();
        session.setSessionId(1L);

        SessionFile saved = new SessionFile();
        saved.setId(8L);
        saved.setFileName("lecture.pdf");
        saved.setFileType("application/pdf");
        saved.setData(new byte[0]);
        saved.setUploadedAt(LocalDateTime.now());
        saved.setSession(session);

        given(sessionService.uploadSessionFile(eq(1L), eq("teacher-token"), any())).willReturn(saved);

        MockMultipartFile file = new MockMultipartFile("file", "lecture.pdf", "application/pdf", "pdf-content".getBytes());

        mockMvc.perform(multipart("/sessions/1/files")
                        .file(file)
                        .header("Authorization", "teacher-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(8)))
                .andExpect(jsonPath("$.fileName", is("lecture.pdf")));
    }

    @Test
    void uploadSessionFile_invalidToken_returns401() throws Exception {
        given(sessionService.uploadSessionFile(eq(1L), eq("invalid-token"), any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        MockMultipartFile file = new MockMultipartFile("file", "lecture.pdf", "application/pdf", "pdf-content".getBytes());

        mockMvc.perform(multipart("/sessions/1/files")
                        .file(file)
                        .header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized());
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
