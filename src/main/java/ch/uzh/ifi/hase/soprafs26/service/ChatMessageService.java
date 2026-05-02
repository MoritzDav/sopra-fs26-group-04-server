package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ChatMessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ChatMessageService
 * Business logic layer for chat message operations.
 */
@Service
@Transactional
public class ChatMessageService {

    private final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public ChatMessageService(@Qualifier("chatMessageRepository") ChatMessageRepository chatMessageRepository,
                             @Qualifier("sessionRepository") SessionRepository sessionRepository,
                             @Qualifier("userRepository") UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save a new chat message to the database.
     * @param sessionId the session ID
     * @param userId the user ID who sent the message
     * @param content the message content
     * @return the saved ChatMessage entity
     */
    public ChatMessage saveMessage(Long sessionId, Long userId, String content) {
        // Validate session exists and is active
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not active");
        }

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate message content
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        if (content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content exceeds maximum length of 2000 characters");
        }

        // Create and save the message
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setUser(user);
        message.setContent(content.trim());
        message.setTimestamp(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("Chat message saved - SessionId: {}, UserId: {}", sessionId, userId);
        return savedMessage;
    }

    /**
     * Get all messages for a specific session.
     * @param sessionId the session ID
     * @return list of chat messages in chronological order
     */
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        return chatMessageRepository.findBySession(session);
    }

    /**
     * Delete all messages for a specific session.
     * Called when the session ends.
     * @param sessionId the session ID
     */
    public void deleteSessionMessages(Long sessionId) {
        chatMessageRepository.deleteBySession_SessionId(sessionId);
        log.info("All chat messages deleted for session: {}", sessionId);
    }

    /**
     * Delete a specific message by ID.
     * @param messageId the message ID
     */
    public void deleteMessage(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        chatMessageRepository.deleteById(messageId);
        log.info("Chat message deleted - MessageId: {}", messageId);
    }

    /**
     * Get a specific message by ID.
     * @param messageId the message ID
     * @return the ChatMessage entity
     */
    public ChatMessage getMessage(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }
}
