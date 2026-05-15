package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        sessionRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void endSession_missingTeacherSnapshot_returns204AndEndsSession() throws Exception {
        User teacher = new User();
        teacher.setFirstName("Teach");
        teacher.setLastName("Er");
        teacher.setUsername("teacher_" + UUID.randomUUID());
        teacher.setPassword("password123");
        teacher.setStatus(UserStatus.ONLINE);
        teacher.setRole(UserRole.TEACHER);
        teacher.setToken("token_" + UUID.randomUUID());
        teacher = userRepository.save(teacher);

        Course course = new Course();
        course.setTitle("Integration Course");
        course.setDescription("desc");
        course.setCourseCode("CC-" + UUID.randomUUID());
        course.setPictureURL("");
        course.setTeacher(teacher);
        course = courseRepository.save(course);

        Session input = new Session();
        input.setTitle("Integration Session");
        Session createdSession = sessionService.startSession(course.getId(), teacher.getToken(), input);

        assertTrue(createdSession.isActive());
        assertNull(createdSession.getTeacherWhiteboard().getCurrentPage().getCanvasSnapshot());

        mockMvc.perform(post("/sessions/{sessionId}/end", createdSession.getSessionId())
                        .header("Authorization", teacher.getToken()))
                .andExpect(status().isNoContent());

        Session endedSession = sessionRepository.findById(createdSession.getSessionId()).orElseThrow();
        assertFalse(endedSession.isActive());
        assertNull(endedSession.getTeacherWhiteboard().getCurrentPage().getBackgroundFile());
    }
}
