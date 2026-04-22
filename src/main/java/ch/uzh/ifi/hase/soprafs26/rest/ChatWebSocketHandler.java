package ch.uzh.ifi.hase.soprafs26.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessagePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ChatMessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatWebSocketHandler
 * Manages WebSocket connections for chat messaging.
 * Handles client connections, message broadcasts, and disconnections on a per-session basis.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final ChatMessageService chatMessageService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    /**
     * Map to store connected WebSocket sessions per chat session.
     * Key: sessionId, Value: Set of WebSocketSessions
     */
    private final Map<String, Set<WebSocketSession>> sessionConnections =
            Collections.synchronizedMap(new ConcurrentHashMap<>());

    /**
     * Map to track which user is connected to which session.
     * Key: WebSocketSession ID, Value: userId
     */
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * Map to track which session each WebSocket is connected to.
     * Key: WebSocketSession ID, Value: sessionId
     */
    private final Map<String, String> websocketSessionMap = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatMessageService chatMessageService,
                                SessionRepository sessionRepository,
                                UserRepository userRepository) {
        this.chatMessageService = chatMessageService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        
        // Initialize ObjectMapper with JSR310 support for LocalDateTime
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();
        String sessionId = extractSessionId(uri);
        Long userId = extractUserId(uri);

        // Validate session and user
        if (sessionRepository.findById(Long.parseLong(sessionId)).isEmpty()) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid session ID"));
            return;
        }

        if (userRepository.findById(userId).isEmpty()) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid user ID"));
            return;
        }

        // Store the connection
        sessionConnections.computeIfAbsent(sessionId, k -> Collections.synchronizedSet(new HashSet<>()))
                .add(session);
        sessionUserMap.put(session.getId(), userId);
        websocketSessionMap.put(session.getId(), sessionId);

        System.out.println("WebSocket chat connection established - SessionId: " + sessionId +
                ", UserId: " + userId + ", WebSocketSession: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = websocketSessionMap.get(session.getId());
        Long userId = sessionUserMap.get(session.getId());

        if (sessionId == null || userId == null) {
            return;
        }

        try {
            // Parse the incoming message
            ChatMessagePostDTO messageDTO = objectMapper.readValue(message.getPayload(), ChatMessagePostDTO.class);

            // Save the message to the database
            ChatMessage chatMessage = chatMessageService.saveMessage(Long.parseLong(sessionId), userId, messageDTO.getContent());

            // Convert to DTO for broadcasting
            ChatMessageGetDTO responseDTO = DTOMapper.INSTANCE.convertChatMessageEntityToGetDTO(chatMessage);

            // Broadcast to all users in this session
            broadcastMessage(sessionId, responseDTO);

        } catch (IOException e) {
            System.err.println("Error parsing chat message: " + e.getMessage());
            session.sendMessage(new TextMessage("{\"error\":\"Invalid message format\"}"));
        } catch (Exception e) {
            System.err.println("Error handling chat message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = websocketSessionMap.remove(session.getId());
        Long userId = sessionUserMap.remove(session.getId());

        if (sessionId != null) {
            Set<WebSocketSession> sessions = sessionConnections.get(sessionId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionConnections.remove(sessionId);
                }
            }
        }

        System.out.println("WebSocket chat connection closed - SessionId: " + sessionId +
                ", UserId: " + userId + ", WebSocketSession: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket chat transport error: " + exception.getMessage());
        exception.printStackTrace();
    }

    /**
     * Broadcast a message to all users connected to a specific session.
     * @param sessionId the session ID
     * @param message the message to broadcast
     */
    private void broadcastMessage(String sessionId, ChatMessageGetDTO message) {
        Set<WebSocketSession> sessions = sessionConnections.get(sessionId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        System.err.println("Error sending message to session " + session.getId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Error serializing message: " + e.getMessage());
        }
    }

    /**
     * Extract the session ID from the WebSocket URI.
     * Expected format: /ws/chat/{sessionId}?userId={userId}
     * @param uri the WebSocket URI
     * @return the session ID
     */
    private String extractSessionId(String uri) {
        try {
            int startIndex = uri.lastIndexOf('/') + 1;
            int endIndex = uri.indexOf('?');
            if (endIndex == -1) {
                endIndex = uri.length();
            }
            return uri.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract the user ID from the WebSocket URI query parameters.
     * Expected format: /ws/chat/{sessionId}?userId={userId}
     * @param uri the WebSocket URI
     * @return the user ID or 0 if not found
     */
    private Long extractUserId(String uri) {
        try {
            int startIndex = uri.indexOf("userId=");
            if (startIndex == -1) {
                return 0L;
            }
            int endIndex = uri.indexOf('&', startIndex);
            if (endIndex == -1) {
                endIndex = uri.length();
            }
            String userIdStr = uri.substring(startIndex + "userId=".length(), endIndex);
            return Long.parseLong(userIdStr);
        } catch (Exception e) {
            return 0L;
        }
    }
}
