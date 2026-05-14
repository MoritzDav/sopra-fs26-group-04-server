package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.constant.WhiteboardMode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTest {

    // BrowniePointEntry

    @Test
    void browniePointEntry_gettersAndSetters() {
        BrowniePointEntry entry = new BrowniePointEntry();
        User student = new User();
        Course course = new Course();
        Session session = new Session();
        LocalDateTime now = LocalDateTime.now();

        entry.setId(1L);
        entry.setStudent(student);
        entry.setCourse(course);
        entry.setSession(session);
        entry.setPoints(5);
        entry.setTimestamp(now);

        assertEquals(1L, entry.getId());
        assertEquals(student, entry.getStudent());
        assertEquals(course, entry.getCourse());
        assertEquals(session, entry.getSession());
        assertEquals(5, entry.getPoints());
        assertEquals(now, entry.getTimestamp());
    }

    // ChatMessage

    @Test
    void chatMessage_defaultConstructor() {
        ChatMessage msg = new ChatMessage();
        assertNotNull(msg);
    }

    @Test
    void chatMessage_parameterizedConstructor() {
        Session session = new Session();
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        ChatMessage msg = new ChatMessage(session, user, "Hello!", now);

        assertEquals(session, msg.getSession());
        assertEquals(user, msg.getUser());
        assertEquals("Hello!", msg.getContent());
        assertEquals(now, msg.getTimestamp());
    }

    @Test
    void chatMessage_gettersAndSetters() {
        ChatMessage msg = new ChatMessage();
        Session session = new Session();
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        msg.setMessageId(10L);
        msg.setSession(session);
        msg.setUser(user);
        msg.setContent("Test message");
        msg.setTimestamp(now);

        assertEquals(10L, msg.getMessageId());
        assertEquals(session, msg.getSession());
        assertEquals(user, msg.getUser());
        assertEquals("Test message", msg.getContent());
        assertEquals(now, msg.getTimestamp());
    }

    // Course

    @Test
    void course_gettersAndSetters() {
        Course course = new Course();
        User teacher = new User();

        course.setId(1L);
        course.setTitle("Math 101");
        course.setDescription("Intro to Math");
        course.setCourseCode("MTH101");
        course.setPictureURL("http://example.com/pic.png");
        course.setTeacher(teacher);

        assertEquals(1L, course.getId());
        assertEquals("Math 101", course.getTitle());
        assertEquals("Intro to Math", course.getDescription());
        assertEquals("MTH101", course.getCourseCode());
        assertEquals("http://example.com/pic.png", course.getPictureURL());
        assertEquals(teacher, course.getTeacher());
    }

    // CourseEnrollment

    @Test
    void courseEnrollment_gettersAndSetters() {
        CourseEnrollment enrollment = new CourseEnrollment();
        LocalDateTime now = LocalDateTime.now();

        enrollment.setId(1L);
        enrollment.setStudentId(2L);
        enrollment.setCourseId(3L);
        enrollment.setJoinedDate(now);

        assertEquals(1L, enrollment.getId());
        assertEquals(2L, enrollment.getStudentId());
        assertEquals(3L, enrollment.getCourseId());
        assertEquals(now, enrollment.getJoinedDate());
    }

    // Session

    @Test
    void session_gettersAndSetters() {
        Session session = new Session();
        Course course = new Course();
        TeacherWhiteboard wb = new TeacherWhiteboard();
        PersonalWhiteboard selected = new PersonalWhiteboard();
        LocalDateTime now = LocalDateTime.now();

        session.setSessionId(1L);
        session.setTitle("Lecture 1");
        session.setActive(true);
        session.setCreatedAt(now);
        session.setStart(now);
        session.setMode(SessionMode.NORMAL);
        session.setCourse(course);
        session.setTeacherWhiteboard(wb);
        session.setSelectedWhiteboard(selected);

        assertEquals(1L, session.getSessionId());
        assertEquals("Lecture 1", session.getTitle());
        assertTrue(session.isActive());
        assertEquals(now, session.getCreatedAt());
        assertEquals(now, session.getStart());
        assertEquals(SessionMode.NORMAL, session.getMode());
        assertEquals(course, session.getCourse());
        assertEquals(wb, session.getTeacherWhiteboard());
        assertEquals(selected, session.getSelectedWhiteboard());
    }

    // SessionFile

    @Test
    void sessionFile_gettersAndSetters() {
        SessionFile file = new SessionFile();
        Session session = new Session();
        LocalDateTime now = LocalDateTime.now();
        byte[] data = {1, 2, 3};

        file.setId(1L);
        file.setFileName("notes.pdf");
        file.setFileType("application/pdf");
        file.setData(data);
        file.setUploadedAt(now);
        file.setSession(session);

        assertEquals(1L, file.getId());
        assertEquals("notes.pdf", file.getFileName());
        assertEquals("application/pdf", file.getFileType());
        assertArrayEquals(data, file.getData());
        assertEquals(now, file.getUploadedAt());
        assertEquals(session, file.getSession());
    }

    // User

    @Test
    void user_gettersAndSetters() {
        User user = new User();

        user.setId(1L);
        user.setFirstName("Anna");
        user.setLastName("Müller");
        user.setUsername("annamueller");
        user.setToken("abc-token");
        user.setPassword("secret");
        user.setStatus(UserStatus.ONLINE);
        user.setRole(UserRole.STUDENT);

        assertEquals(1L, user.getId());
        assertEquals("Anna", user.getFirstName());
        assertEquals("Müller", user.getLastName());
        assertEquals("annamueller", user.getUsername());
        assertEquals("abc-token", user.getToken());
        assertEquals("secret", user.getPassword());
        assertEquals(UserStatus.ONLINE, user.getStatus());
        assertEquals(UserRole.STUDENT, user.getRole());
    }

    // Whiteboard

    @Test
    void whiteboard_gettersAndSetters() {
        PersonalWhiteboard wb = new PersonalWhiteboard();
        WhiteboardPage page = new WhiteboardPage();

        wb.setWhiteboardId(1L);
        wb.setLocked(true);
        wb.setMode(WhiteboardMode.COLLABORATIVE);
        wb.setTeacherLayerReadOnly(true);

        assertEquals(1L, wb.getWhiteboardId());
        assertTrue(wb.isLocked());
        assertEquals(WhiteboardMode.COLLABORATIVE, wb.getMode());
        assertTrue(wb.isTeacherLayerReadOnly());
    }

    @Test
    void whiteboard_addPage_addsAndLinksPage() {
        PersonalWhiteboard wb = new PersonalWhiteboard();
        WhiteboardPage page = new WhiteboardPage();
        page.setPageNumber(1);

        wb.addPage(page);

        assertEquals(1, wb.getPages().size());
        assertEquals(wb, page.getWhiteboard());
    }

    @Test
    void whiteboard_setPages_replacesPageList() {
        PersonalWhiteboard wb = new PersonalWhiteboard();
        WhiteboardPage page = new WhiteboardPage();

        wb.setPages(java.util.List.of(page));

        assertEquals(1, wb.getPages().size());
    }

    // WhiteboardPage

    @Test
    void whiteboardPage_gettersAndSetters() {
        WhiteboardPage page = new WhiteboardPage();
        PersonalWhiteboard wb = new PersonalWhiteboard();

        page.setId(1L);
        page.setPageNumber(2);
        page.setBackgroundFile("background.png");
        page.setCanvasSnapshot("snapshot-data");
        page.setWhiteboard(wb);

        assertEquals(1L, page.getId());
        assertEquals(2, page.getPageNumber());
        assertEquals("background.png", page.getBackgroundFile());
        assertEquals("snapshot-data", page.getCanvasSnapshot());
        assertEquals(wb, page.getWhiteboard());
    }

    // PersonalWhiteboard

    @Test
    void personalWhiteboard_gettersAndSetters() {
        PersonalWhiteboard wb = new PersonalWhiteboard();
        User owner = new User();
        Session session = new Session();
        TeacherWhiteboard background = new TeacherWhiteboard();
        WhiteboardPage page = new WhiteboardPage();

        wb.setOwner(owner);
        wb.setVisible(true);
        wb.setBackgroundContent(background);
        wb.setSession(session);
        wb.setCurrentPage(page);

        assertEquals(owner, wb.getOwner());
        assertTrue(wb.isVisible());
        assertEquals(background, wb.getBackgroundContent());
        assertEquals(session, wb.getSession());
        assertEquals(page, wb.getCurrentPage());
    }

    // TeacherWhiteboard

    @Test
    void teacherWhiteboard_gettersAndSetters() {
        TeacherWhiteboard wb = new TeacherWhiteboard();
        WhiteboardPage page = new WhiteboardPage();
        Session session = new Session();

        wb.setShared(true);
        wb.setCurrentPage(page);
        wb.setSession(session);

        assertTrue(wb.isShared());
        assertEquals(page, wb.getCurrentPage());
        assertEquals(session, wb.getSession());
    }
}
