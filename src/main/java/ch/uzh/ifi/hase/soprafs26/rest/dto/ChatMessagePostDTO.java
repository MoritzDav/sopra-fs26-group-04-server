package ch.uzh.ifi.hase.soprafs26.rest.dto;

/**
 * ChatMessagePostDTO
 * Data Transfer Object for creating/sending chat messages via API or WebSocket.
 */
public class ChatMessagePostDTO {
    
    private String content;

    // Constructors
    public ChatMessagePostDTO() {
    }

    public ChatMessagePostDTO(String content) {
        this.content = content;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
