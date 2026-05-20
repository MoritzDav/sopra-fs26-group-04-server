package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.SessionWebSocketHandler;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatMessageService chatMessageService;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private WhiteboardPageRepository whiteboardPageRepository;
    @Mock private PersonalWhiteboardRepository personalWhiteboardRepository;
    @Mock private SessionFileRepository sessionFileRepository;
    @Mock private SessionWebSocketHandler sessionWebSocketHandler;
    @Mock private GeminiSummaryService geminiSummaryService;

    @InjectMocks
    private SessionService sessionService;

    private User teacher;
    private User student;
    private Course course;
    private Session session;
    private WhiteboardPage teacherPage;

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

        course = new Course();
        course.setId(10L);
        course.setTeacher(teacher);

        session = new Session();
        session.setSessionId(1L);
        session.setActive(true);
        session.setCourse(course);

        TeacherWhiteboard teacherWhiteboard = new TeacherWhiteboard();
        teacherPage = new WhiteboardPage();
        teacherPage.setPageNumber(1);
        teacherPage.setCanvasSnapshot("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2wA8YAAAAASUVORK5CYII=");
        teacherWhiteboard.addPage(teacherPage);
        teacherWhiteboard.setCurrentPage(teacherPage);
        session.setTeacherWhiteboard(teacherWhiteboard);
    }

    /**
     * startSession
     */

    @Test
    void startSession_validTeacher_success() {
        Session input = new Session();
        input.setTitle("Test Session");

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(sessionRepository.save(any())).thenReturn(session);

        Session result = sessionService.startSession(10L, "teacher-token", input);

        assertNotNull(result);
        verify(sessionRepository).save(any());
    }

    @Test
    void startSession_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.startSession(10L, "invalid-token", new Session()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void startSession_notTeacher_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.startSession(10L, "student-token", new Session()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void startSession_courseNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.startSession(99L, "teacher-token", new Session()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void startSession_notCourseOwner_throwsForbidden() {
        User otherTeacher = new User();
        otherTeacher.setId(3L);
        otherTeacher.setToken("other-token");
        otherTeacher.setRole(UserRole.TEACHER);

        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherTeacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.startSession(10L, "other-token", new Session()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    /**
     * endSession
     */

    @Test
    void endSession_validTeacher_success() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        sessionService.endSession(1L, "teacher-token");

        assertFalse(session.isActive());
        verify(sessionRepository).save(session);
        verify(whiteboardPageRepository).save(argThat(page ->
                page.getBackgroundFile() != null && page.getBackgroundFile().startsWith("data:application/pdf;base64,")));
        verify(chatMessageService).deleteSessionMessages(1L);
    }

    @Test
    void endSession_missingSnapshot_stillEndsSession() {
        teacherPage.setCanvasSnapshot(null);
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        sessionService.endSession(1L, "teacher-token");

        assertFalse(session.isActive());
        verify(sessionRepository).save(session);
        verify(whiteboardPageRepository, never()).save(any());
        verify(chatMessageService).deleteSessionMessages(1L);
    }

    @Test
    void endSession_invalidSnapshot_stillEndsSession() {
        teacherPage.setCanvasSnapshot("not-base64-image");
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        sessionService.endSession(1L, "teacher-token");

        assertFalse(session.isActive());
        verify(sessionRepository).save(session);
        verify(whiteboardPageRepository, never()).save(any());
        verify(chatMessageService).deleteSessionMessages(1L);
    }

    @Test
    void endSession_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.endSession(1L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void endSession_notTeacher_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.endSession(1L, "student-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void endSession_sessionNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.endSession(99L, "teacher-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void endSession_notSessionOwner_throwsForbidden() {
        User otherTeacher = new User();
        otherTeacher.setId(3L);
        otherTeacher.setToken("other-token");
        otherTeacher.setRole(UserRole.TEACHER);

        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherTeacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.endSession(1L, "other-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    /**
     * getSessionsByCourse
     */

    @Test
    void getSessionsByCourse_validTeacher_success() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(sessionRepository.findByCourseId(10L)).thenReturn(List.of(session));

        List<Session> result = sessionService.getSessionsByCourse(10L, "teacher-token");

        assertEquals(1, result.size());
    }

    @Test
    void getSessionsByCourse_validStudent_success() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(2L, 10L))
                .thenReturn(Optional.of(new CourseEnrollment()));
        when(sessionRepository.findByCourseId(10L)).thenReturn(List.of(session));

        List<Session> result = sessionService.getSessionsByCourse(10L, "student-token");

        assertEquals(1, result.size());
    }

    @Test
    void getSessionsByCourse_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.getSessionsByCourse(10L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void getSessionsByCourse_courseNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.getSessionsByCourse(99L, "teacher-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getSessionsByCourse_notPartOfCourse_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(2L, 10L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.getSessionsByCourse(10L, "student-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    /**
     * toggleCollaboration
     */

    @Test
    void toggleCollaboration_enableByTeacher_success() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        sessionService.toggleCollaboration(1L, "teacher-token", true);

        assertEquals(SessionMode.MULTI_MODE, session.getMode());
        verify(sessionRepository).save(session);
        verify(sessionWebSocketHandler).broadcastCollaborationStart("1", null, null);
    }

    @Test
    void toggleCollaboration_disableByTeacher_success() {
        session.setMode(SessionMode.MULTI_MODE);
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        sessionService.toggleCollaboration(1L, "teacher-token", false);

        assertEquals(SessionMode.NORMAL, session.getMode());
        verify(sessionRepository).save(session);
        verify(sessionWebSocketHandler).broadcastCollaborationEnd("1");
    }

    @Test
    void toggleCollaboration_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.toggleCollaboration(1L, "invalid-token", true));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void toggleCollaboration_notTeacher_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.toggleCollaboration(1L, "student-token", true));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void toggleCollaboration_notSessionOwner_throwsForbidden() {
        User otherTeacher = new User();
        otherTeacher.setId(3L);
        otherTeacher.setToken("other-token");
        otherTeacher.setRole(UserRole.TEACHER);

        when(userRepository.findByToken("other-token")).thenReturn(Optional.of(otherTeacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.toggleCollaboration(1L, "other-token", true));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void summarizeSessionFileToPdf_success_returnsPdf() throws Exception {
        SessionFile file = new SessionFile();
        file.setId(5L);
        file.setFileType("application/pdf");
        file.setData(createTestPdfBytes("This is a test PDF content."));
        file.setSession(session);

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionFileRepository.findByIdAndSessionSessionId(5L, 1L)).thenReturn(Optional.of(file));
        when(geminiSummaryService.summarizeText(any())).thenReturn("Short summary");

        byte[] result = sessionService.summarizeSessionFileToPdf(1L, 5L, "teacher-token");

        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(geminiSummaryService).summarizeText(any());
    }

    /**
     * joinSession
     */

    @Test
    void joinSession_validStudent_createsWhiteboard() {
        session.setActive(true);
        PersonalWhiteboard whiteboard = new PersonalWhiteboard();
        whiteboard.setWhiteboardId(5L);

        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(2L, 10L))
                .thenReturn(Optional.of(new CourseEnrollment()));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.empty());
        when(personalWhiteboardRepository.save(any())).thenReturn(whiteboard);

        PersonalWhiteboard result = sessionService.joinSession(10L, 1L, "student-token");

        assertNotNull(result);
        verify(personalWhiteboardRepository).save(any());
    }

    @Test
    void joinSession_alreadyJoined_returnsExistingWhiteboard() {
        PersonalWhiteboard existing = new PersonalWhiteboard();
        existing.setWhiteboardId(5L);

        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(2L, 10L))
                .thenReturn(Optional.of(new CourseEnrollment()));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.of(existing));

        PersonalWhiteboard result = sessionService.joinSession(10L, 1L, "student-token");

        assertEquals(existing, result);
        verify(personalWhiteboardRepository, never()).save(any());
    }

    @Test
    void joinSession_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.joinSession(10L, 1L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void joinSession_notStudent_throwsForbidden() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.joinSession(10L, 1L, "teacher-token"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void joinSession_sessionNotActive_throwsBadRequest() {
        session.setActive(false);
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.joinSession(10L, 1L, "student-token"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void joinSession_notEnrolled_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(courseEnrollmentRepository.findByStudentIdAndCourseId(2L, 10L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.joinSession(10L, 1L, "student-token"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * getWhiteboardState
     */

    @Test
    void getWhiteboardState_withSnapshot_returnsSnapshot() {
        WhiteboardPage page = new WhiteboardPage();
        page.setCanvasSnapshot("canvas-data");

        TeacherWhiteboard wb = new TeacherWhiteboard();
        wb.setCurrentPage(page);
        session.setTeacherWhiteboard(wb);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertEquals("canvas-data", sessionService.getWhiteboardState(1L).getCanvasSnapshot());
    }

    @Test
    void getWhiteboardState_sessionNotFound_throwsNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.getWhiteboardState(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getWhiteboardState_noWhiteboard_returnsNullSnapshot() {
        session.setTeacherWhiteboard(null);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertNull(sessionService.getWhiteboardState(1L).getCanvasSnapshot());
    }

    /**
     * saveWhiteboardState
     */

    @Test
    void saveWhiteboardState_valid_savesSnapshot() {
        WhiteboardPage page = new WhiteboardPage();
        TeacherWhiteboard wb = new TeacherWhiteboard();
        wb.setCurrentPage(page);
        session.setTeacherWhiteboard(wb);

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        sessionService.saveWhiteboardState(1L, "teacher-token", "new-snapshot");

        assertEquals("new-snapshot", page.getCanvasSnapshot());
        verify(whiteboardPageRepository).save(page);
    }

    @Test
    void saveWhiteboardState_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.saveWhiteboardState(1L, "invalid-token", "snap"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void saveWhiteboardState_notTeacher_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.saveWhiteboardState(1L, "student-token", "snap"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void saveWhiteboardState_noWhiteboard_throwsNotFound() {
        session.setTeacherWhiteboard(null);
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.saveWhiteboardState(1L, "teacher-token", "snap"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * savePersonalWhiteboardState
     */

    @Test
    void savePersonalWhiteboardState_valid_savesSnapshot() {
        WhiteboardPage page = new WhiteboardPage();
        PersonalWhiteboard whiteboard = new PersonalWhiteboard();
        whiteboard.setCurrentPage(page);

        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.of(whiteboard));

        sessionService.savePersonalWhiteboardState(10L, 1L, "student-token", "my-snap");

        assertEquals("my-snap", page.getCanvasSnapshot());
        verify(whiteboardPageRepository).save(page);
    }

    @Test
    void savePersonalWhiteboardState_notStudent_throwsForbidden() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.savePersonalWhiteboardState(10L, 1L, "teacher-token", "snap"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void savePersonalWhiteboardState_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.savePersonalWhiteboardState(10L, 1L, "invalid-token", "snap"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void savePersonalWhiteboardState_whiteboardNotFound_throwsNotFound() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.savePersonalWhiteboardState(10L, 1L, "student-token", "snap"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * getStudentWhiteboardState
     */

    @Test
    void getStudentWhiteboardState_valid_returnsSnapshot() {
        WhiteboardPage page = new WhiteboardPage();
        page.setCanvasSnapshot("student-canvas");
        PersonalWhiteboard whiteboard = new PersonalWhiteboard();
        whiteboard.setCurrentPage(page);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.of(whiteboard));

        assertEquals("student-canvas", sessionService.getStudentWhiteboardState(1L, 2L).getCanvasSnapshot());
    }

    @Test
    void getStudentWhiteboardState_sessionNotFound_throwsNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.getStudentWhiteboardState(99L, 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getStudentWhiteboardState_whiteboardNotFound_throwsNotFound() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.getStudentWhiteboardState(1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * selectStudentBoard
     */

    @Test
    void selectStudentBoard_valid_setsStudentModeAndBroadcasts() {
        PersonalWhiteboard whiteboard = new PersonalWhiteboard();
        whiteboard.setWhiteboardId(7L);

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.of(whiteboard));

        sessionService.selectStudentBoard(10L, 1L, "teacher-token", 2L);

        assertEquals(SessionMode.STUDENT, session.getMode());
        verify(sessionRepository).save(session);
        verify(sessionWebSocketHandler).broadcastCollaborationStart("1", 2L, 7L);
    }

    @Test
    void selectStudentBoard_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.selectStudentBoard(10L, 1L, "invalid-token", 2L));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void selectStudentBoard_notTeacher_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.selectStudentBoard(10L, 1L, "student-token", 2L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void selectStudentBoard_studentNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.selectStudentBoard(10L, 1L, "teacher-token", 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void selectStudentBoard_targetIsTeacher_throwsBadRequest() {
        User anotherTeacher = new User();
        anotherTeacher.setId(5L);
        anotherTeacher.setRole(UserRole.TEACHER);

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(5L)).thenReturn(Optional.of(anotherTeacher));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.selectStudentBoard(10L, 1L, "teacher-token", 5L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void selectStudentBoard_whiteboardNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.selectStudentBoard(10L, 1L, "teacher-token", 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * deselectStudentBoard
     */

    @Test
    void deselectStudentBoard_valid_resetsNormalModeAndBroadcasts() {
        WhiteboardPage page = new WhiteboardPage();
        PersonalWhiteboard whiteboard = new PersonalWhiteboard();
        whiteboard.setCurrentPage(page);
        session.setMode(SessionMode.STUDENT);

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.of(whiteboard));

        sessionService.deselectStudentBoard(10L, 1L, "teacher-token", 2L, "final-snap");

        assertEquals(SessionMode.NORMAL, session.getMode());
        assertEquals("final-snap", page.getCanvasSnapshot());
        verify(sessionWebSocketHandler).broadcastCollaborationEnd("1");
    }

    @Test
    void deselectStudentBoard_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.deselectStudentBoard(10L, 1L, "invalid-token", 2L, "snap"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void deselectStudentBoard_notTeacher_throwsForbidden() {
        when(userRepository.findByToken("student-token")).thenReturn(Optional.of(student));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.deselectStudentBoard(10L, 1L, "student-token", 2L, "snap"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deselectStudentBoard_whiteboardNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(personalWhiteboardRepository.findByOwnerIdAndSessionSessionId(2L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.deselectStudentBoard(10L, 1L, "teacher-token", 2L, "snap"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * getSessionFiles
     */

    @Test
    void getSessionFiles_valid_returnsList() {
        SessionFile file = new SessionFile();
        file.setId(1L);

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionFileRepository.findBySessionSessionId(1L)).thenReturn(List.of(file));

        List<SessionFile> result = sessionService.getSessionFiles(1L, "teacher-token");

        assertEquals(1, result.size());
    }

    @Test
    void getSessionFiles_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.getSessionFiles(1L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void getSessionFiles_sessionNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.getSessionFiles(99L, "teacher-token"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * uploadSessionFile
     */

    @Test
    void uploadSessionFile_validPdf_saved() throws Exception {
        SessionFile saved = new SessionFile();
        saved.setId(1L);
        saved.setSession(session);

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionFileRepository.save(any())).thenReturn(saved);

        SessionFile result = sessionService.uploadSessionFile(1L, "teacher-token", mockFile);

        assertNotNull(result);
        verify(sessionFileRepository).save(any());
    }

    @Test
    void uploadSessionFile_invalidToken_throwsUnauthorized() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.uploadSessionFile(1L, "invalid-token", mockFile));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void uploadSessionFile_nonPdfFile_throwsBadRequest() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("image/png");

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.uploadSessionFile(1L, "teacher-token", mockFile));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    /**
     * summarizeSessionFileToPdf (additional)
     */

    @Test
    void summarizeSessionFileToPdf_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.summarizeSessionFileToPdf(1L, 5L, "invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void summarizeSessionFileToPdf_notPdfFile_throwsBadRequest() {
        SessionFile file = new SessionFile();
        file.setId(5L);
        file.setFileType("image/png");
        file.setSession(session);

        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionFileRepository.findByIdAndSessionSessionId(5L, 1L)).thenReturn(Optional.of(file));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                sessionService.summarizeSessionFileToPdf(1L, 5L, "teacher-token"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void summarizeSessionFileToPdf_fileNotFound_throwsNotFound() {
        when(userRepository.findByToken("teacher-token")).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionFileRepository.findByIdAndSessionSessionId(5L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                sessionService.summarizeSessionFileToPdf(1L, 5L, "teacher-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private byte[] createTestPdfBytes(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}