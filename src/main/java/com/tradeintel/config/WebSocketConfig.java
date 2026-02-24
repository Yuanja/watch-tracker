package com.tradeintel.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration for real-time updates.
 *
 * <p>Clients connect to {@code /ws} (with SockJS fallback) and can subscribe to:
 * <ul>
 *   <li>{@code /topic/*} — broadcast destinations (e.g. new listings in a group)</li>
 *   <li>{@code /queue/*} — point-to-point user-specific destinations (e.g. personal
 *       notification alerts, review queue updates)</li>
 * </ul>
 *
 * <p>Application messages are sent to destinations prefixed with {@code /app}. The
 * server forwards them to the appropriate broker destination after processing.
 *
 * <p>The in-memory broker is sufficient for the expected small user base
 * (no Redis pub/sub required per CLAUDE.md). All async processing events that
 * need WebSocket push use {@code SimpMessagingTemplate} to publish to these
 * destinations from {@code @Service} classes.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LogManager.getLogger(WebSocketConfig.class);

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Allow connections from the Vite dev server and production origin.
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "https://*.tradeintel.com"
                )
                // SockJS fallback for browsers that do not support native WebSocket.
                .withSockJS();

        log.info("STOMP WebSocket endpoint registered at /ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages routed to @MessageMapping controller methods.
        registry.setApplicationDestinationPrefixes("/app");

        // Enable the in-memory broker for /topic and /queue destinations.
        // /topic — fan-out (e.g. "new listing in group X")
        // /queue — point-to-point (e.g. user-specific notification)
        registry.enableSimpleBroker("/topic", "/queue");

        // User-specific destination prefix, e.g. /user/{userId}/queue/notifications.
        registry.setUserDestinationPrefix("/user");
    }
}
