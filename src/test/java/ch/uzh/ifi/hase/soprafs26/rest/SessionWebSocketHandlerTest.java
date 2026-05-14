package ch.uzh.ifi.hase.soprafs26.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SessionWebSocketHandlerTest {

    private SessionWebSocketHandler handler;
    private ObjectMapper objectMapper;

    private WebSocketSession session1;
    private WebSocketSession session2;
    private WebSocketSession session3;

    @BeforeEach
    void setup() {
        handler = new SessionWebSocketHandler();
        objectMapper = new ObjectMapper();

        session1 = createMockSession("ws1", "http://localhost:8080/ws/session/42");
        session2 = createMockSession("ws2", "http://localhost:8080/ws/session/42");
        session3 = createMockSession("ws3", "http://localhost:8080/ws/session/99");
    }

    // ── Connection management ────────────────────────────────────────────────

    @Test
    void afterConnectionEstablished_sessionReceivesBroadcast() throws Exception {
        handler.afterConnectionEstablished(session1);

        handler.broadcastCollaborationStart("42", 1L, 2L);

        verify(session1, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionEstablished_twoSessionsSameId_bothReceiveBroadcast() throws Exception {
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        handler.broadcastCollaborationStart("42", 1L, 2L);

        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionEstablished_twoSessionsDifferentIds_isolatedBroadcast() throws Exception {
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session3);

        handler.broadcastCollaborationStart("42", 1L, 2L);

        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session3, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosed_sessionNoLongerReceivesBroadcast() throws Exception {
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        handler.broadcastCollaborationStart("42", 1L, 2L);

        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosed_lastSession_groupCleaned_noBroadcastSent() throws Exception {
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        // Should silently do nothing — no NPE or exception
        assertDoesNotThrow(() -> handler.broadcastCollaborationStart("42", 1L, 2L));
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosed_otherSessionsInGroupUnaffected() throws Exception {
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        handler.broadcastCollaborationStart("42", 1L, 2L);

        verify(session1, never()).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTransportError_doesNotThrow() {
        assertDoesNotThrow(() ->
                handler.handleTransportError(session1, new RuntimeException("connection reset")));
    }

    // ── broadcastCollaborationStart ──────────────────────────────────────────

    @Test
    void broadcastCollaborationStart_noConnections_nothingSent() throws Exception {
        assertDoesNotThrow(() -> handler.broadcastCollaborationStart("42", 1L, 2L));
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcastCollaborationStart_correctJsonPayload() throws Exception {
        handler.afterConnectionEstablished(session1);

        handler.broadcastCollaborationStart("42", 7L, 3L);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(captor.capture());

        Map<String, Object> payload = objectMapper.readValue(captor.getValue().getPayload(), Map.class);
        assertEquals("collaboration-start", payload.get("type"));
        assertEquals("STUDENT", payload.get("mode"));
        assertEquals(7, payload.get("studentId"));
        assertEquals(3, payload.get("whiteboardId"));
    }

    @Test
    void broadcastCollaborationStart_closedSessionSkipped() throws Exception {
        when(session1.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        handler.broadcastCollaborationStart("42", 1L, 2L);

        verify(session1, never()).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastCollaborationStart_ioException_doesNotThrow() throws Exception {
        handler.afterConnectionEstablished(session1);
        doThrow(new IOException("send failed")).when(session1).sendMessage(any());

        assertDoesNotThrow(() -> handler.broadcastCollaborationStart("42", 1L, 2L));
    }

    // ── broadcastCollaborationEnd ────────────────────────────────────────────

    @Test
    void broadcastCollaborationEnd_sendsToAllConnectedSessions() throws Exception {
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        handler.broadcastCollaborationEnd("42");

        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcastCollaborationEnd_correctJsonPayload() throws Exception {
        handler.afterConnectionEstablished(session1);

        handler.broadcastCollaborationEnd("42");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(captor.capture());

        Map<String, Object> payload = objectMapper.readValue(captor.getValue().getPayload(), Map.class);
        assertEquals("collaboration-end", payload.get("type"));
        assertEquals("NORMAL", payload.get("mode"));
    }

    @Test
    void broadcastCollaborationEnd_noConnections_nothingSent() throws Exception {
        assertDoesNotThrow(() -> handler.broadcastCollaborationEnd("42"));
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    // ── broadcastStudentBoardSelected / Deselected ───────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void broadcastStudentBoardSelected_sendsCollaborationStartEvent() throws Exception {
        handler.afterConnectionEstablished(session1);

        handler.broadcastStudentBoardSelected("42", 5L, 10L);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(captor.capture());

        Map<String, Object> payload = objectMapper.readValue(captor.getValue().getPayload(), Map.class);
        assertEquals("collaboration-start", payload.get("type"));
        assertEquals(5, payload.get("studentId"));
        assertEquals(10, payload.get("whiteboardId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcastStudentBoardDeselected_sendsCollaborationEndEvent() throws Exception {
        handler.afterConnectionEstablished(session1);

        handler.broadcastStudentBoardDeselected("42");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(captor.capture());

        Map<String, Object> payload = objectMapper.readValue(captor.getValue().getPayload(), Map.class);
        assertEquals("collaboration-end", payload.get("type"));
    }

    // ── URL parsing (indirect via afterConnectionEstablished) ────────────────

    @Test
    void extractSessionId_withQueryParams_parsedCorrectly() throws Exception {
        WebSocketSession sessionWithQuery = createMockSession("wsQ", "http://localhost:8080/ws/session/55?token=abc");
        handler.afterConnectionEstablished(sessionWithQuery);

        handler.broadcastCollaborationStart("55", 1L, 2L);

        verify(sessionWithQuery, times(1)).sendMessage(any(TextMessage.class));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private WebSocketSession createMockSession(String id, String uri) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        try {
            when(session.getUri()).thenReturn(new URI(uri));
        } catch (Exception e) {
            // will not happen
        }
        return session;
    }
}
