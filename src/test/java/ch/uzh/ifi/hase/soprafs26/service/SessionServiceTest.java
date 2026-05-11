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

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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