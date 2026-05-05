package ch.uzh.ifi.hase.soprafs26.rest;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<String, Set<WebSocketSession>> sessionConnections =
            Collections.synchronizedMap(new ConcurrentHashMap<>());

    private final Map<String, String> websocketSessionMap = new ConcurrentHashMap<>();

    public SessionWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();
        String sessionId = extractSessionId(uri);

        sessionConnections.computeIfAbsent(sessionId, k -> Collections.synchronizedSet(new HashSet<>()))
                .add(session);
        websocketSessionMap.put(session.getId(), sessionId);

        System.out.println("WebSocket session connection established - SessionId: " + sessionId +
                ", WebSocketSession: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = websocketSessionMap.remove(session.getId());

        if (sessionId != null) {
            Set<WebSocketSession> sessions = sessionConnections.get(sessionId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionConnections.remove(sessionId);
                }
            }
        }

        System.out.println("WebSocket session connection closed - SessionId: " + sessionId +
                ", WebSocketSession: " + session.getId());
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket session transport error: " + exception.getMessage());
    }

    /**
     * Broadcast student-board-selected event to all participants in a session.
     * @param sessionId the session ID
     * @param studentId the selected student ID
     * @param whiteboardId the selected whiteboard ID
     */
    public void broadcastStudentBoardSelected(String sessionId, Long studentId, Long whiteboardId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "student-board-selected");
        event.put("studentId", studentId);
        event.put("whiteboardId", whiteboardId);
        broadcastEvent(sessionId, event);
    }

    /**
     * Broadcast student-board-deselected event to all participants in a session.
     * @param sessionId the session ID
     */
    public void broadcastStudentBoardDeselected(String sessionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "student-board-deselected");
        broadcastEvent(sessionId, event);
    }

    private void broadcastEvent(String sessionId, Map<String, Object> event) {
        Set<WebSocketSession> sessions = sessionConnections.get(sessionId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        System.err.println("Error sending event to session " + session.getId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Error serializing event: " + e.getMessage());
        }
    }

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




}
