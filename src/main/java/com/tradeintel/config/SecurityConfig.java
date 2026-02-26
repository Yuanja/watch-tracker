package com.tradeintel.config;

import com.tradeintel.auth.GoogleOAuth2UserService;
import com.tradeintel.auth.JwtAuthFilter;
import com.tradeintel.auth.OAuth2SuccessHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the WhatsApp Trade Intelligence Platform.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li><b>Stateless sessions</b> — no HTTP session is created; all auth state
 *       is carried in a signed JWT Bearer token.</li>
 *   <li><b>CSRF disabled for webhooks only</b> — {@code /api/webhooks/**} uses
 *       HMAC-SHA256 signature validation instead of CSRF tokens. All other
 *       endpoints retain CSRF protection where applicable; however, because the
 *       SPA sends tokens via the {@code Authorization} header (not cookies),
 *       CSRF is globally disabled for the API tier and the SPA relies on the
 *       Same-Origin / CORS policy enforced here.</li>
 *   <li><b>URL-based authorization</b> per PRD section 3.3:
 *       <ul>
 *         <li>Public — {@code /api/webhooks/**}, {@code /api/auth/**}</li>
 *         <li>Authenticated — {@code /api/chat/**}, {@code /api/listings/**},
 *             {@code /api/notifications/**}, {@code /api/messages/**}</li>
 *         <li>Admin+ — {@code /api/review/**}, {@code /api/normalize/**},
 *             {@code /api/jargon/**}</li>
 *         <li>Uber-admin only — {@code /api/admin/**}</li>
 *       </ul>
 *   </li>
 *   <li><b>Method-level security</b> enabled via {@link EnableMethodSecurity} so
 *       that {@code @AdminOnly} / {@code @UberAdminOnly} meta-annotations work.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LogManager.getLogger(SecurityConfig.class);

    private final JwtAuthFilter            jwtAuthFilter;
    private final GoogleOAuth2UserService  oAuth2UserService;
    private final OAuth2SuccessHandler     oAuth2SuccessHandler;
    private final List<String>             allowedOrigins;

    public SecurityConfig(JwtAuthFilter           jwtAuthFilter,
                          GoogleOAuth2UserService  oAuth2UserService,
                          OAuth2SuccessHandler     oAuth2SuccessHandler,
                          @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
                          List<String>             allowedOrigins) {
        this.jwtAuthFilter       = jwtAuthFilter;
        this.oAuth2UserService   = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.allowedOrigins      = allowedOrigins;
    }

    // -------------------------------------------------------------------------
    // Main security filter chain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring Spring Security filter chain");

        http
            // CORS — must come before CSRF so that pre-flight OPTIONS are handled first.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF — disabled for the entire API tier. The SPA authenticates via
            // Bearer token (Authorization header), not cookies, so CSRF is not a
            // meaningful threat vector. Webhook endpoints use HMAC-SHA256 signatures.
            .csrf(AbstractHttpConfigurer::disable)

            // No HTTP session — stateless JWT-based auth.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL-based authorization rules (order matters — most specific first).
            .authorizeHttpRequests(auth -> auth

                // Public: webhook receiver (signature-validated in WhapiWebhookController)
                .requestMatchers("/api/webhooks/**").permitAll()

                // Public: OAuth2 login redirect and token exchange
                .requestMatchers("/api/auth/**").permitAll()

                // Public: Spring Security OAuth2 login/callback endpoints
                .requestMatchers("/login/**", "/oauth2/**").permitAll()

                // Public: WebSocket upgrade endpoint
                .requestMatchers("/ws/**").permitAll()

                // Public: Actuator health check (useful for load balancer probes)
                .requestMatchers("/actuator/health").permitAll()

                // Admin+ only
                .requestMatchers("/api/review/**").hasRole("ADMIN")
                .requestMatchers("/api/normalize/**").hasRole("ADMIN")
                .requestMatchers("/api/jargon/**").hasRole("ADMIN")

                // Uber-admin only
                .requestMatchers("/api/admin/**").hasRole("UBER_ADMIN")

                // Everything else requires at minimum a valid authenticated session
                .requestMatchers(
                        "/api/chat/**",
                        "/api/listings/**",
                        "/api/notifications/**",
                        "/api/messages/**"
                ).authenticated()

                // Catch-all — any unmatched API request requires authentication
                .anyRequest().authenticated()
            )

            // REST API: return 401 instead of redirecting to OAuth2 login page.
            // Without this, unauthenticated requests to /api/** get a 302 redirect
            // which the SPA's axios interceptor cannot handle properly.
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")
                )
            )

            // OAuth2 login configuration
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
            )

            // Inject the JWT filter before the standard username/password filter.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // -------------------------------------------------------------------------
    // CORS
    // -------------------------------------------------------------------------

    /**
     * Configures CORS to allow the React dev server ({@code localhost:5173})
     * during local development. In production the allowed origins should be
     * set via {@code app.cors.allowed-origins} and injected here.
     *
     * <p>The WebSocket upgrade path ({@code /ws/**}) is also covered so that
     * STOMP connections from the browser are not blocked.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Whapi-Signature"
        ));

        // Required for the SPA to read response headers (e.g., Location after redirects).
        config.setExposedHeaders(List.of("Authorization", "Location"));

        // Allow cookies for OAuth2 state cookie during the login flow.
        config.setAllowCredentials(true);

        // Cache pre-flight response for 30 minutes.
        config.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // -------------------------------------------------------------------------
    // Password encoder
    // -------------------------------------------------------------------------

    /**
     * BCrypt password encoder. Although this application uses Google SSO and
     * never stores passwords, a {@link PasswordEncoder} bean is required by some
     * Spring Security internals (e.g., DaoAuthenticationProvider auto-configuration).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
