package ch.uzh.ifi.hase.soprafs26.rest;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WhiteboardDrawingMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


 //Manages client connections per course and broadcasts drawing events.

@Component
public class WhiteboardWebSocketHandler extends TextWebSocketHandler {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Map to store connected sessions per course.
     * Key: courseId, Value: Set of WebSocketSessions
     */
    private final Map<String, Set<WebSocketSession>> courseConnections = 
        Collections.synchronizedMap(new HashMap<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();
        String courseId = extractCourseId(uri);
        
        // Store the session
        courseConnections.computeIfAbsent(courseId, k -> Collections.synchronizedSet(new HashSet<>()))
                .add(session);
        
        System.out.println("WebSocket connection established for course: " + courseId + 
                         ", session: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String uri = session.getUri().toString();
        String courseId = extractCourseId(uri);
        
        // Remove the session from the course connections
        Set<WebSocketSession> sessions = courseConnections.get(courseId);
        if (sessions != null) {
            sessions.remove(session);
            
            // Clean up empty course connections
            if (sessions.isEmpty()) {
                courseConnections.remove(courseId);
            }
        }
        
        System.out.println("WebSocket connection closed for course: " + courseId + 
                         ", session: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String uri = session.getUri().toString();
        String courseId = extractCourseId(uri);
        
        try {
            // Parse drawing message
            WhiteboardDrawingMessage drawingMessage = objectMapper.readValue(
                message.getPayload(), 
                WhiteboardDrawingMessage.class
            );
            
            // Broadcast to all connected clients in the same course
            broadcastMessage(courseId, message.getPayload());
            
        } catch (Exception e) {
            System.err.println("Error processing drawing message: " + e.getMessage());
        }
    }

    /**
     * Broadcast a message to all connected clients in a specific course.
     * @param courseId the course ID
     * @param messagePayload the message to broadcast
     */
    private void broadcastMessage(String courseId, String messagePayload) {
        Set<WebSocketSession> sessions = courseConnections.get(courseId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(messagePayload);
        
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    System.err.println("Error sending message to session " + session.getId() + 
                                     ": " + e.getMessage());
                }
            }
        }
    }
   

    private String extractCourseId(String uri) {
        // URI format: file://localhost/ws/whiteboard/{courseId}
        String[] parts = uri.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "unknown";
    }

    //Get the number of connected sessions for a specific course.
   
    public int getConnectedCount(String courseId) {
        Set<WebSocketSession> sessions = courseConnections.get(courseId);
        return sessions != null ? sessions.size() : 0;
    }

    // Get all course IDs with active connections.
    public Set<String> getActiveCourses() {
        return new HashSet<>(courseConnections.keySet());
    }
}
