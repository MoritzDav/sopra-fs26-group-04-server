package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ChatMessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private User user;
    private Course course;
    private Session session;
    private ChatMessage chatMessage;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(UserRole.STUDENT);

        course = new Course();
        course.setId(1L);
        course.setTitle("Test Course");

        session = new Session();
        session.setSessionId(1L);
        session.setCourse(course);
        session.setActive(true);
        session.setTitle("Test Session");

        chatMessage = new ChatMessage();
        chatMessage.setMessageId(1L);
        chatMessage.setSession(session);
        chatMessage.setUser(user);
        chatMessage.setContent("Hello, this is a test message");
        chatMessage.setTimestamp(LocalDateTime.now());
    }

    @Test
    public void testSaveMessage_Success() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // Act
        ChatMessage savedMessage = chatMessageService.saveMessage(1L, 1L, "Hello, this is a test message");

        // Assert
        assertNotNull(savedMessage);
        assertEquals("Hello, this is a test message", savedMessage.getContent());
        assertEquals(user.getId(), savedMessage.getUser().getId());
        assertEquals(session.getSessionId(), savedMessage.getSession().getSessionId());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    public void testSaveMessage_SessionNotFound() {
        // Arrange
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.saveMessage(999L, 1L, "Test message");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void testSaveMessage_UserNotFound() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.saveMessage(1L, 999L, "Test message");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void testSaveMessage_EmptyContent() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.saveMessage(1L, 1L, "");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void testSaveMessage_ContentExceedsMaxLength() {
        // Arrange
        String longContent = "a".repeat(2001);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.saveMessage(1L, 1L, longContent);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void testSaveMessage_SessionNotActive() {
        // Arrange
        session.setActive(false);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.saveMessage(1L, 1L, "Test message");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void testGetSessionMessages_Success() {
        // Arrange
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(chatMessage);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(chatMessageRepository.findBySession(session)).thenReturn(messages);

        // Act
        List<ChatMessage> retrievedMessages = chatMessageService.getSessionMessages(1L);

        // Assert
        assertNotNull(retrievedMessages);
        assertEquals(1, retrievedMessages.size());
        assertEquals("Hello, this is a test message", retrievedMessages.get(0).getContent());
        verify(chatMessageRepository, times(1)).findBySession(session);
    }

    @Test
    public void testGetSessionMessages_SessionNotFound() {
        // Arrange
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.getSessionMessages(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void testDeleteSessionMessages_Success() {
        // Arrange
        doNothing().when(chatMessageRepository).deleteBySession_SessionId(1L);

        // Act
        chatMessageService.deleteSessionMessages(1L);

        // Assert
        verify(chatMessageRepository, times(1)).deleteBySession_SessionId(1L);
    }

    @Test
    public void testDeleteSessionMessages_SessionWithNoMessages() {
        // Arrange - delete session that has no messages (no error expected)
        doNothing().when(chatMessageRepository).deleteBySession_SessionId(999L);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> chatMessageService.deleteSessionMessages(999L));
        verify(chatMessageRepository, times(1)).deleteBySession_SessionId(999L);
    }

    @Test
    public void testCannotSaveMessageAfterSessionEnds() {
        // Arrange - session is no longer active
        session.setActive(false);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.saveMessage(1L, 1L, "This should fail");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("not active"));
    }

    @Test
    public void testDeleteSessionMessages_MultipleTimes() {
        // Arrange
        doNothing().when(chatMessageRepository).deleteBySession_SessionId(1L);

        // Act - call delete multiple times (idempotent operation)
        chatMessageService.deleteSessionMessages(1L);
        chatMessageService.deleteSessionMessages(1L);
        chatMessageService.deleteSessionMessages(1L);

        // Assert - verify each deletion was processed
        verify(chatMessageRepository, times(3)).deleteBySession_SessionId(1L);
    }

    @Test
    public void testDeleteMessage_Success() {
        // Arrange
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(chatMessage));

        // Act
        chatMessageService.deleteMessage(1L);

        // Assert
        verify(chatMessageRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteMessage_MessageNotFound() {
        // Arrange
        when(chatMessageRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.deleteMessage(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void testGetMessage_Success() {
        // Arrange
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(chatMessage));

        // Act
        ChatMessage retrievedMessage = chatMessageService.getMessage(1L);

        // Assert
        assertNotNull(retrievedMessage);
        assertEquals("Hello, this is a test message", retrievedMessage.getContent());
        verify(chatMessageRepository, times(1)).findById(1L);
    }

    @Test
    public void testGetMessage_MessageNotFound() {
        // Arrange
        when(chatMessageRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatMessageService.getMessage(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
