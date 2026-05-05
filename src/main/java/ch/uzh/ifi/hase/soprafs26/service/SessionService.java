package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.rest.SessionWebSocketHandler;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionBoardSelectDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.repository.*;
import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WhiteboardStateDTO;

@Service
@Transactional
public class SessionService {

    private final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final WhiteboardPageRepository whiteboardPageRepository;
    private final ChatMessageService chatMessageService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final SessionWebSocketHandler sessionWebSocketHandler;


    public SessionService(@Qualifier("sessionRepository") SessionRepository sessionRepository,
                          @Qualifier("courseRepository") CourseRepository courseRepository,
                          @Qualifier("userRepository") UserRepository userRepository,
                          @Qualifier("whiteboardPageRepository") WhiteboardPageRepository whiteboardPageRepository,
                          @Qualifier("chatMessageService") ChatMessageService chatMessageService,
                          @Qualifier("courseEnrollmentRepository") CourseEnrollmentRepository courseEnrollmentRepository,
                          SessionWebSocketHandler sessionWebSocketHandler) {
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.whiteboardPageRepository = whiteboardPageRepository;
        this.chatMessageService = chatMessageService;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.sessionWebSocketHandler = sessionWebSocketHandler;
    }

    //Create and start session
    public Session startSession(Long courseId, String token, Session sessionInput) {

        User user = userRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        if (user.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers are allowed to start a session");
        }

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No course with that courseId found"));

        if (!course.getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not owner of this course");
        }

        Session session = new Session();
        session.setTitle(sessionInput.getTitle());
        session.setMode(SessionMode.NORMAL);
        session.setCourse(course);
        session.setCreatedAt(LocalDateTime.now());
        session.setActive(true);
        session.setStart(LocalDateTime.now());

        TeacherWhiteboard teacherWhiteboard = new TeacherWhiteboard();
        teacherWhiteboard.setShared(false);
        teacherWhiteboard.setLocked(false);
        teacherWhiteboard.setTeacherLayerReadOnly(true);

        WhiteboardPage firstPage = new WhiteboardPage();
        firstPage.setPageNumber(1);
        firstPage.setWhiteboard(teacherWhiteboard);

        teacherWhiteboard.addPage(firstPage);
        teacherWhiteboard.setCurrentPage(firstPage);
        session.setTeacherWhiteboard(teacherWhiteboard);

        session = sessionRepository.save(session);
        sessionRepository.flush();

        log.debug("Created session: {}", session.getSessionId());
        return session;
    }

    public WhiteboardStateDTO getWhiteboardState(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        TeacherWhiteboard wb = session.getTeacherWhiteboard();
        if (wb != null && wb.getCurrentPage() != null) {
            dto.setCanvasSnapshot(wb.getCurrentPage().getCanvasSnapshot());
        }
        return dto;
    }

    public void saveWhiteboardState(Long sessionId, String token, String canvasSnapshot) {
        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        TeacherWhiteboard wb = session.getTeacherWhiteboard();
        if (wb == null || wb.getCurrentPage() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Whiteboard page not found");
        }

        WhiteboardPage page = wb.getCurrentPage();
        page.setCanvasSnapshot(canvasSnapshot);
        whiteboardPageRepository.save(page);
    }

    //End session
    public void endSession(Long sessionId, String token) {
        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        session.setActive(false);
        chatMessageService.deleteSessionMessages(sessionId);

        sessionRepository.save(session);
        sessionRepository.flush();
        log.debug("Ended session {}", sessionId);
    }

    //Display sessions in a course dashboard
    public List<Session> getSessionsByCourse(Long courseId, String token) {
        User user = getUserByToken(token);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isTeacher = course.getTeacher().getId().equals(user.getId());
        boolean isStudent = courseEnrollmentRepository.findByStudentIdAndCourseId(user.getId(), course.getId()).isPresent();

        if (!isTeacher && !isStudent) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this course");
        }

        return sessionRepository.findByCourseId(courseId);
    }

    //Select student whiteboard and broadcast it
    public void selectStudentBoard(Long courseId, Long sessionId, String token, Long studentId){

        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a student");
        }

        session.setMode(SessionMode.STUDENT);
        sessionRepository.save(session);
        sessionRepository.flush();

        sessionWebSocketHandler.broadcastStudentBoardSelected(sessionId.toString(), studentId, null);
        log.debug("Teacher selected student {} board in session {}", studentId, sessionId);
    }

    public void deselectStudentBoard(Long courseId, Long sessionId, String token) {
        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        session.setMode(SessionMode.NORMAL);
        session.setSelectedWhiteboard(null);
        sessionRepository.save(session);
        sessionRepository.flush();

        sessionWebSocketHandler.broadcastStudentBoardDeselected(sessionId.toString());
        log.debug("Teacher deselected student board in session {}", sessionId);
    }


    //Helper functions
    // Fetch user via token including validation
    private User getUserByToken(String token) {
        return userRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
    }

    // Check if user is a teacher
    private void validateTeacher(User user) {
        if (user.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can do this");
        }
    }

    // Fetch session by ID
    private Session getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    // Check if teacher owns the session
    private void validateSessionOwner(Session session, User user) {
        if (!session.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the teacher of this session");
        }
    }
}