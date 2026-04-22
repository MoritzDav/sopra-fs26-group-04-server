package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ChatMessageGetDTO
 * Data Transfer Object for retrieving chat messages via API.
 */
public class ChatMessageGetDTO {
    
    private Long messageId;
    private Long sessionId;
    private Long userId;
    private String username;
    private String content;
    
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Constructors
    public ChatMessageGetDTO() {
    }

    public ChatMessageGetDTO(Long messageId, Long sessionId, Long userId, String username, String content, LocalDateTime timestamp) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
