package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ChatMessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatMessageServiceIntegrationTest
 * Integration tests for chat message service with real database operations.
 * Tests the complete cleanup workflow when sessions end.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ChatMessageServiceIntegrationTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private User teacher;
    private User student;
    private Course course;
    private Session session;

    @BeforeEach
    public void setup() {
        // Create a teacher user
        teacher = new User();
        teacher.setUsername("inttest_teacher");
        teacher.setFirstName("Test");
        teacher.setLastName("Teacher");
        teacher.setRole(UserRole.TEACHER);
        teacher.setStatus(UserStatus.ONLINE);
        teacher.setPassword("password123");
        teacher.setToken(UUID.randomUUID().toString());
        teacher = userRepository.save(teacher);

        // Create a student user
        student = new User();
        student.setUsername("inttest_student");
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setRole(UserRole.STUDENT);
        student.setStatus(UserStatus.ONLINE);
        student.setPassword("password123");
        student.setToken(UUID.randomUUID().toString());
        student = userRepository.save(student);

        // Create a course with the teacher
        course = new Course();
        course.setTitle("Integration Test Course");
        course.setDescription("Course for testing chat cleanup");
        course.setCourseCode("INTTEST01");
        course.setTeacher(teacher);
        course = courseRepository.save(course);

        // Create an active session
        session = new Session();
        session.setCourse(course);
        session.setTitle("Integration Test Session");
        session.setActive(true);
        session.setStart(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        session = sessionRepository.save(session);
    }

    @Test
    public void testChatMessagesDeletedWhenSessionEnds() {
        // Arrange - save multiple chat messages
        ChatMessage msg1 = new ChatMessage();
        msg1.setSession(session);
        msg1.setUser(student);
        msg1.setContent("First message in session");
        msg1.setTimestamp(LocalDateTime.now());
        msg1 = chatMessageRepository.save(msg1);

        ChatMessage msg2 = new ChatMessage();
        msg2.setSession(session);
        msg2.setUser(teacher);
        msg2.setContent("Second message in session");
        msg2.setTimestamp(LocalDateTime.now());
        msg2 = chatMessageRepository.save(msg2);

        ChatMessage msg3 = new ChatMessage();
        msg3.setSession(session);
        msg3.setUser(student);
        msg3.setContent("Third message in session");
        msg3.setTimestamp(LocalDateTime.now());
        msg3 = chatMessageRepository.save(msg3);

        // Verify messages exist before cleanup
        List<ChatMessage> messagesBefore = chatMessageService.getSessionMessages(session.getSessionId());
        assertEquals(3, messagesBefore.size(), "Should have 3 messages before cleanup");

        // Act - delete all messages for the session (as would happen when session ends)
        chatMessageService.deleteSessionMessages(session.getSessionId());

        // Assert - messages should be deleted
        List<ChatMessage> messagesAfter = chatMessageService.getSessionMessages(session.getSessionId());
        assertEquals(0, messagesAfter.size(), "All messages should be deleted after session ends");
    }

    @Test
    public void testChatCleanupOnlyAffectsSpecificSession() {
        // Arrange - create two sessions
        Session session2 = new Session();
        session2.setCourse(course);
        session2.setTitle("Second Test Session");
        session2.setActive(true);
        session2.setStart(LocalDateTime.now());
        session2.setCreatedAt(LocalDateTime.now());
        session2 = sessionRepository.save(session2);

        // Add messages to both sessions
        ChatMessage msg1 = new ChatMessage();
        msg1.setSession(session);
        msg1.setUser(student);
        msg1.setContent("Message in session 1");
        msg1.setTimestamp(LocalDateTime.now());
        msg1 = chatMessageRepository.save(msg1);

        ChatMessage msg2 = new ChatMessage();
        msg2.setSession(session2);
        msg2.setUser(student);
        msg2.setContent("Message in session 2");
        msg2.setTimestamp(LocalDateTime.now());
        msg2 = chatMessageRepository.save(msg2);

        // Verify both have messages
        assertEquals(1, chatMessageService.getSessionMessages(session.getSessionId()).size());
        assertEquals(1, chatMessageService.getSessionMessages(session2.getSessionId()).size());

        // Act - delete messages from only the first session
        chatMessageService.deleteSessionMessages(session.getSessionId());

        // Assert - first session should be empty, second should still have the message
        assertEquals(0, chatMessageService.getSessionMessages(session.getSessionId()).size(),
                "Session 1 messages should be deleted");
        assertEquals(1, chatMessageService.getSessionMessages(session2.getSessionId()).size(),
                "Session 2 messages should still exist");
    }

    @Test
    public void testEmptySessionCleanupIsIdempotent() {
        // Act - delete messages from a session that has no messages (should not error)
        assertDoesNotThrow(() -> chatMessageService.deleteSessionMessages(session.getSessionId()),
                "Cleanup of empty session should not throw exception");

        // Clean up again to verify idempotency
        assertDoesNotThrow(() -> chatMessageService.deleteSessionMessages(session.getSessionId()),
                "Second cleanup of empty session should also not throw exception");
    }

    @Test
    public void testCannotSaveMessageToInactiveSession() {
        // Arrange - create a message first in active session
        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setUser(student);
        msg.setContent("Valid message");
        msg.setTimestamp(LocalDateTime.now());
        msg = chatMessageRepository.save(msg);

        // Verify message was saved
        assertEquals(1, chatMessageService.getSessionMessages(session.getSessionId()).size());

        // Act - deactivate the session and try to save a new message
        session.setActive(false);
        sessionRepository.save(session);

        // Assert - should not be able to save message to inactive session
        assertThrows(Exception.class, () -> {
            chatMessageService.saveMessage(session.getSessionId(), student.getId(), "Should fail");
        });
    }

    @Test
    public void testMultipleUsersMessagesDeletedTogether() {
        // Arrange - add messages from multiple users
        ChatMessage msg1 = new ChatMessage();
        msg1.setSession(session);
        msg1.setUser(teacher);
        msg1.setContent("Teacher's message");
        msg1.setTimestamp(LocalDateTime.now());
        msg1 = chatMessageRepository.save(msg1);

        ChatMessage msg2 = new ChatMessage();
        msg2.setSession(session);
        msg2.setUser(student);
        msg2.setContent("Student's message");
        msg2.setTimestamp(LocalDateTime.now());
        msg2 = chatMessageRepository.save(msg2);

        // Verify both users have messages
        List<ChatMessage> messages = chatMessageService.getSessionMessages(session.getSessionId());
        assertEquals(2, messages.size());
        assertTrue(messages.stream().anyMatch(m -> m.getUser().getId().equals(teacher.getId())));
        assertTrue(messages.stream().anyMatch(m -> m.getUser().getId().equals(student.getId())));

        // Act - delete all messages
        chatMessageService.deleteSessionMessages(session.getSessionId());

        // Assert - all messages deleted regardless of user
        assertEquals(0, chatMessageService.getSessionMessages(session.getSessionId()).size());
    }

    @Test
    public void testLargeBatchOfMessagesDeleted() {
        // Arrange - create many messages
        int messageCount = 50;
        for (int i = 0; i < messageCount; i++) {
            ChatMessage msg = new ChatMessage();
            msg.setSession(session);
            msg.setUser(student);
            msg.setContent("Message " + (i + 1) + " in batch");
            msg.setTimestamp(LocalDateTime.now().plusSeconds(i));
            chatMessageRepository.save(msg);
        }

        // Verify all messages exist
        List<ChatMessage> messagesBefore = chatMessageService.getSessionMessages(session.getSessionId());
        assertEquals(messageCount, messagesBefore.size());

        // Act - delete all messages at once
        chatMessageService.deleteSessionMessages(session.getSessionId());

        // Assert - all messages should be deleted
        List<ChatMessage> messagesAfter = chatMessageService.getSessionMessages(session.getSessionId());
        assertEquals(0, messagesAfter.size());
    }

    @Test
    public void testDeleteMessageByIdAfterSessionCleanup() {
        // Arrange - save a message and then cleanup the session
        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setUser(student);
        msg.setContent("Individual message");
        msg.setTimestamp(LocalDateTime.now());
        msg = chatMessageRepository.save(msg);
        Long messageId = msg.getMessageId();

        // Act - cleanup session
        chatMessageService.deleteSessionMessages(session.getSessionId());

        // Assert - message should be gone, individual delete should fail
        List<ChatMessage> remaining = chatMessageService.getSessionMessages(session.getSessionId());
        assertEquals(0, remaining.size());

        // Verify we can't get the deleted message by ID
        assertThrows(Exception.class, () -> chatMessageService.getMessage(messageId));
    }
}
