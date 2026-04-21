package ch.uzh.ifi.hase.soprafs26.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WhiteboardDrawingMessage;

import java.io.IOException;
import java.net.URI;

/**
 * Unit tests for WhiteboardWebSocketHandler.
 * Tests message broadcasting, connection management, and error handling.
 */
public class WhiteboardWebSocketHandlerTest {

    private WhiteboardWebSocketHandler webSocketHandler;

    private WebSocketSession mockSession1;
    private WebSocketSession mockSession2;
    private WebSocketSession mockSession3;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        webSocketHandler = new WhiteboardWebSocketHandler();
        mockSession1 = createMockSession("1", "http://localhost:8080/ws/whiteboard/1");
        mockSession2 = createMockSession("2", "http://localhost:8080/ws/whiteboard/1");
        mockSession3 = createMockSession("3", "http://localhost:8080/ws/whiteboard/2");
    }

    @Test
    public void testAfterConnectionEstablished_AddsSessionToConnections() throws Exception {
        // When
        webSocketHandler.afterConnectionEstablished(mockSession1);

        // Then
        assertEquals(1, webSocketHandler.getConnectedCount("1"));
        assertTrue(webSocketHandler.getActiveCourses().contains("1"));
    }

    @Test
    public void testAfterConnectionEstablished_MultipleSessions_InSameCourse() throws Exception {
        // When
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession2);

        // Then
        assertEquals(2, webSocketHandler.getConnectedCount("1"));
    }

    @Test
    public void testAfterConnectionEstablished_MultipleSessions_DifferentCourses() throws Exception {
        // When
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession3);

        // Then
        assertEquals(1, webSocketHandler.getConnectedCount("1"));
        assertEquals(1, webSocketHandler.getConnectedCount("2"));
        assertEquals(2, webSocketHandler.getActiveCourses().size());
    }

    @Test
    public void testHandleTextMessage_BroadcastsMessageToAllClients() throws Exception {
        // Given
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession2);

        WhiteboardDrawingMessage drawingMessage = new WhiteboardDrawingMessage(1L, 100L, "draw");
        drawingMessage.setX(10.5);
        drawingMessage.setY(20.5);
        drawingMessage.setColor("#FF0000");
        drawingMessage.setSize(5);

        String messageJson = objectMapper.writeValueAsString(drawingMessage);
        TextMessage textMessage = new TextMessage(messageJson);

        // When
        webSocketHandler.handleTextMessage(mockSession1, textMessage);

        // Then - Verify messages were sent to all clients in the course
        verify(mockSession1, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(mockSession2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    public void testHandleTextMessage_DoesNotBroadcastToOtherCourses() throws Exception {
        // Given
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession2);
        webSocketHandler.afterConnectionEstablished(mockSession3);

        WhiteboardDrawingMessage drawingMessage = new WhiteboardDrawingMessage(1L, 100L, "draw");
        String messageJson = objectMapper.writeValueAsString(drawingMessage);
        TextMessage textMessage = new TextMessage(messageJson);

        // When
        webSocketHandler.handleTextMessage(mockSession1, textMessage);

        // Then - Only session3 should NOT be called (it's in a different course)
        verify(mockSession3, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    public void testHandleTextMessage_SendsToOpenSessionsOnly() throws Exception {
        // Given
        when(mockSession2.isOpen()).thenReturn(false);
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession2);

        WhiteboardDrawingMessage drawingMessage = new WhiteboardDrawingMessage(1L, 100L, "draw");
        String messageJson = objectMapper.writeValueAsString(drawingMessage);
        TextMessage textMessage = new TextMessage(messageJson);

        // When
        webSocketHandler.handleTextMessage(mockSession1, textMessage);

        // Then - Only open sessions should receive the message
        verify(mockSession1, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(mockSession2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    public void testHandleTextMessage_HandlesIOException() throws Exception {
        // Given - Create a session that will throw IOException on send
        WebSocketSession exceptionSession = createMockSession("2-ex", "http://localhost:8080/ws/whiteboard/1");
        doThrow(new IOException("Send failed")).when(exceptionSession).sendMessage(any());
        
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(exceptionSession);

        WhiteboardDrawingMessage drawingMessage = new WhiteboardDrawingMessage(1L, 100L, "draw");
        String messageJson = objectMapper.writeValueAsString(drawingMessage);
        TextMessage textMessage = new TextMessage(messageJson);

        // When/Then - Should not throw exception, just handle it gracefully
        assertDoesNotThrow(() -> webSocketHandler.handleTextMessage(mockSession1, textMessage));
    }

    @Test
    public void testAfterConnectionClosed_RemovesSessionFromConnections() throws Exception {
        // Given
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession2);
        assertEquals(2, webSocketHandler.getConnectedCount("1"));

        // When
        webSocketHandler.afterConnectionClosed(mockSession1, null);

        // Then
        assertEquals(1, webSocketHandler.getConnectedCount("1"));
    }

    @Test
    public void testAfterConnectionClosed_RemovesEmptyCoursesFromActiveCourses() throws Exception {
        // Given
        webSocketHandler.afterConnectionEstablished(mockSession1);
        assertTrue(webSocketHandler.getActiveCourses().contains("1"));

        // When
        webSocketHandler.afterConnectionClosed(mockSession1, null);

        // Then
        assertFalse(webSocketHandler.getActiveCourses().contains("1"));
    }

    @Test
    public void testAfterConnectionClosed_PreservesOtherCoursesAndSessions() throws Exception {
        // Given
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession2);
        webSocketHandler.afterConnectionEstablished(mockSession3);
        assertEquals(2, webSocketHandler.getConnectedCount("1"));
        assertEquals(1, webSocketHandler.getConnectedCount("2"));

        // When
        webSocketHandler.afterConnectionClosed(mockSession1, null);

        // Then
        assertEquals(1, webSocketHandler.getConnectedCount("1"));
        assertEquals(1, webSocketHandler.getConnectedCount("2"));
    }

    @Test
    public void testGetConnectedCount_ReturnsZeroForUnknownCourse() {
        // When/Then
        assertEquals(0, webSocketHandler.getConnectedCount("999"));
    }

    @Test
    public void testGetActiveCourses_ReturnsEmptySetWhenNoConnections() {
        // When/Then
        assertTrue(webSocketHandler.getActiveCourses().isEmpty());
    }

    @Test
    public void testGetActiveCourses_ReturnsAllActiveCourses() throws Exception {
        // Given
        webSocketHandler.afterConnectionEstablished(mockSession1);
        webSocketHandler.afterConnectionEstablished(mockSession3);

        // When
        var activeCourses = webSocketHandler.getActiveCourses();

        // Then
        assertEquals(2, activeCourses.size());
        assertTrue(activeCourses.contains("1"));
        assertTrue(activeCourses.contains("2"));
    }

    // Helper method to create mock WebSocket session
    private WebSocketSession createMockSession(String id, String uri) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        try {
            when(session.getUri()).thenReturn(new URI(uri));
        } catch (Exception e) {
            // Should not happen in tests
        }
        return session;
    }
}
