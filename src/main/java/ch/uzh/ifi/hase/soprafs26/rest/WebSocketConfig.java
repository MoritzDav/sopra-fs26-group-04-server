package ch.uzh.ifi.hase.soprafs26.rest;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;


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

    @Bean
    @ConditionalOnWebApplication
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean() {
            @Override
            public void afterPropertiesSet() {
                try {
                    super.afterPropertiesSet();
                } catch (IllegalStateException e) {
                    // ServerContainer not available in mock test environments; no-op
                }
            }
        };
        container.setMaxTextMessageBufferSize(512 * 1024);
        container.setMaxBinaryMessageBufferSize(512 * 1024);
        return container;
    }
}
