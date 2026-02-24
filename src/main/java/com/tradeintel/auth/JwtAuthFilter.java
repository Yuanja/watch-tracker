package com.tradeintel.auth;

import com.tradeintel.common.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Servlet filter that extracts a Bearer JWT from the {@code Authorization} header,
 * validates it, loads the corresponding user, and populates the
 * {@link SecurityContextHolder} for the duration of the request.
 *
 * <p>Processing steps:
 * <ol>
 *   <li>Extract the raw token from the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Delegate validation to {@link JwtTokenProvider}.</li>
 *   <li>Load the {@link User} from the database by the UUID encoded in the token subject.</li>
 *   <li>Construct a {@link UsernamePasswordAuthenticationToken} and place it in the
 *       {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <p>If any step fails the request continues unauthenticated; downstream security rules
 * determine whether access is granted.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserRepository   userRepository;

    public JwtAuthFilter(JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.tokenProvider  = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        try {
            String token = extractBearerToken(request);

            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                UUID userId = tokenProvider.getUserIdFromToken(token);

                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent() && Boolean.TRUE.equals(userOpt.get().getIsActive())) {
                    UserPrincipal principal = UserPrincipal.from(userOpt.get());

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.debug("JWT user {} not found or inactive — treating request as anonymous",
                            userId);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Parses the raw token value from the Authorization header.
     *
     * @param request the incoming HTTP request
     * @return the raw JWT string, or {@code null} if no Bearer token is present
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
