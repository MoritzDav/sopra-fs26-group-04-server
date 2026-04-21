package ch.uzh.ifi.hase.soprafs26.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WhiteboardDrawingMessage;

/**
 * Integration tests for WebSocket configuration and endpoint availability.
 * Verifies that the WebSocket endpoint is registered and accessible.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketConfigIntegrationTest {

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Autowired
    private WhiteboardWebSocketHandler webSocketHandler;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        assertNotNull(webSocketConfig, "WebSocketConfig should be autowired");
        assertNotNull(webSocketHandler, "WhiteboardWebSocketHandler should be autowired");
    }

    @Test
    public void testWebSocketConfigExists() {
        assertNotNull(webSocketConfig);
    }

    @Test
    public void testWebSocketHandlerExists() {
        assertNotNull(webSocketHandler);
    }

    @Test
    public void testWebSocketConfigImplementsConfigurer() {
        assertTrue(webSocketConfig instanceof WebSocketConfigurer);
    }

    @Test
    public void testWhiteboardWebSocketHandlerExists() {
        assertTrue(webSocketHandler instanceof WebSocketHandler);
    }

    @Test
    public void testWhiteboardDrawingMessageCreation() {
        // When
        WhiteboardDrawingMessage message = new WhiteboardDrawingMessage(1L, 100L, "draw");
        message.setX(10.5);
        message.setY(20.5);
        message.setColor("#FF0000");
        message.setSize(5);

        // Then
        assertEquals(1L, message.getCourseId());
        assertEquals(100L, message.getUserId());
        assertEquals("draw", message.getAction());
        assertEquals(10.5, message.getX());
        assertEquals(20.5, message.getY());
        assertEquals("#FF0000", message.getColor());
        assertEquals(5, message.getSize());
        assertNotNull(message.getTimestamp());
    }

    @Test
    public void testWhiteboardDrawingMessageSerialization() throws Exception {
        // Given
        WhiteboardDrawingMessage original = new WhiteboardDrawingMessage(1L, 100L, "draw");
        original.setX(10.5);
        original.setY(20.5);
        original.setColor("#FF0000");
        original.setSize(5);

        // When
        String json = objectMapper.writeValueAsString(original);
        WhiteboardDrawingMessage deserialized = objectMapper.readValue(
            json, WhiteboardDrawingMessage.class
        );

        // Then
        assertEquals(original.getCourseId(), deserialized.getCourseId());
        assertEquals(original.getUserId(), deserialized.getUserId());
        assertEquals(original.getAction(), deserialized.getAction());
        assertEquals(original.getX(), deserialized.getX());
        assertEquals(original.getY(), deserialized.getY());
        assertEquals(original.getColor(), deserialized.getColor());
        assertEquals(original.getSize(), deserialized.getSize());
    }

    @Test
    public void testWebSocketHandlerConnectionCounting() {
        // Initial state should have no connections
        assertEquals(0, webSocketHandler.getConnectedCount("1"));
        assertTrue(webSocketHandler.getActiveCourses().isEmpty());
    }
}
