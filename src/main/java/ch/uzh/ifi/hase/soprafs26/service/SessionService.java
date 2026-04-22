package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.*;

@Service
@Transactional
public class SessionService {

    private final Logger log = LoggerFactory.getLogger(SessionService.class);
    
    private final SessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ChatMessageService chatMessageService;

    public SessionService(@Qualifier("sessionRepository") SessionRepository sessionRepository,
                          @Qualifier("courseRepository") CourseRepository courseRepository,
                          @Qualifier("userRepository") UserRepository userRepository,
                          @Qualifier("chatMessageService") ChatMessageService chatMessageService) {
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.chatMessageService = chatMessageService;
    }



    //Create and start session
    public Session startSession(Long courseId, String token, Session sessionInput){

        //Validate token of user, that creates the session
        User user = userRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        
        //Check whether user is actually a teacher
        if (user.getRole() != UserRole.TEACHER){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers are allowed to start a session");
        }

        //Fetch the course
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No course with that courseId found"));


        //Check whether teacher owns this course
        if(!course.getTeacher().getId().equals(user.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not owner of this course");
        }

        //Create new session
        Session session = new Session();
        session.setTitle(sessionInput.getTitle());
        session.setMode(SessionMode.NORMAL);
        session.setCourse(course);
        session.setCreatedAt(LocalDateTime.now());
        session.setActive(true);
        session.setStart(LocalDateTime.now());

        //Create teacher whiteboard
        TeacherWhiteboard teacherWhiteboard = new TeacherWhiteboard();
        teacherWhiteboard.setShared(false);
        teacherWhiteboard.setLocked(false);
        teacherWhiteboard.setTeacherLayerReadOnly(true);

        //Create first whiteboard page
        WhiteboardPage firstPage = new WhiteboardPage();
        firstPage.setPageNumber(1);
        firstPage.setWhiteboard(teacherWhiteboard);

        //Connect Whiteboard with page
        teacherWhiteboard.addPage(firstPage);
        teacherWhiteboard.setCurrentPage(firstPage);
        session.setTeacherWhiteboard(teacherWhiteboard);

        //saving and persisting in database
        session = sessionRepository.save(session);
        sessionRepository.flush();

        log.debug("Created session: {}", session.getSessionId());
        return session;

    }

    //End session
    public void endSession(Long sessionId, String token){

        //Validate user via token
        User user = userRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        //Check if user is a teacher
        if (user.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can end a session");
        }

        //Fetch session
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        //Check if teacher matches session
        if (!session.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the teacher of this session");
        }

        session.setActive(false);

        // Delete all chat messages associated with this session
        chatMessageService.deleteSessionMessages(sessionId);

        sessionRepository.save(session);
        sessionRepository.flush();
        log.debug("Ended session {}", sessionId);
    }
}
