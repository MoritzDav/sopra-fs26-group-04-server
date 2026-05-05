package ch.uzh.ifi.hase.soprafs26.rest;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


 // Enables WebSocket support and registers the whiteboard handler.

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WhiteboardWebSocketHandler whiteboardWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final SessionWebSocketHandler sessionWebSocketHandler;

    public WebSocketConfig(WhiteboardWebSocketHandler whiteboardWebSocketHandler,
                          ChatWebSocketHandler chatWebSocketHandler,
                           SessionWebSocketHandler sessionWebSocketHandler) {
        this.whiteboardWebSocketHandler = whiteboardWebSocketHandler;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.sessionWebSocketHandler = sessionWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(whiteboardWebSocketHandler, "/ws/whiteboard/{courseId}")
                .setAllowedOrigins("*");
        registry.addHandler(chatWebSocketHandler, "/ws/chat/{sessionId}")
                .setAllowedOrigins("*");
        registry.addHandler(sessionWebSocketHandler, "/ws/session/{sessionId}")
                .setAllowedOrigins("*");
    }
}
