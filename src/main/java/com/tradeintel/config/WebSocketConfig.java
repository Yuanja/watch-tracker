package com.tradeintel.config;

import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserPrincipal;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.UUID;

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
 * <p>JWT authentication is performed on STOMP CONNECT via a channel interceptor
 * that extracts the token from the {@code Authorization} header or from the
 * {@code token} query parameter on the SockJS handshake URL.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LogManager.getLogger(WebSocketConfig.class);

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public WebSocketConfig(JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        log.info("STOMP WebSocket endpoint registered at /ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = extractToken(accessor);
                    if (token != null && tokenProvider.validateToken(token)) {
                        try {
                            UUID userId = tokenProvider.getUserIdFromToken(token);
                            User user = userRepository.findById(userId).orElse(null);
                            if (user != null) {
                                UserPrincipal principal = UserPrincipal.from(user);
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                principal, null, principal.getAuthorities());
                                accessor.setUser(auth);
                                log.debug("WebSocket CONNECT authenticated for userId={}", userId);
                            }
                        } catch (Exception e) {
                            log.warn("WebSocket JWT authentication failed: {}", e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }

    /**
     * Extracts the JWT token from either the STOMP Authorization header
     * or the SockJS URL query parameter.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Try STOMP native header first
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try token from native headers (sent by frontend as custom STOMP header)
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isBlank()) {
            return tokenHeader;
        }

        return null;
    }
}
