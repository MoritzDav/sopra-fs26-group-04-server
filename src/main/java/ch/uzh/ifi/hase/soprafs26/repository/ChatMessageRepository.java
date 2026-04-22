package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ChatMessageRepository
 * Database access layer for ChatMessage entities.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Find all chat messages for a specific session.
     * @param session the session to find messages for
     * @return list of chat messages in the session
     */
    List<ChatMessage> findBySession(Session session);
    
    /**
     * Find all chat messages for a specific session ID.
     * @param sessionId the session ID
     * @return list of chat messages in the session
     */
    List<ChatMessage> findBySession_SessionId(Long sessionId);
    
    /**
     * Delete all chat messages for a specific session.
     * @param session the session whose messages should be deleted
     */
    void deleteBySession(Session session);
    
    /**
     * Delete all chat messages for a specific session ID.
     * @param sessionId the session ID
     */
    void deleteBySession_SessionId(Long sessionId);
}
