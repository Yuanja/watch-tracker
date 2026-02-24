package com.tradeintel.auth;

import com.tradeintel.common.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles a successful Google OAuth2 authentication by generating a JWT and
 * redirecting the browser to the React SPA with the token as a query parameter.
 *
 * <p>The React app reads the {@code token} query parameter on the
 * {@code /auth/callback} route, stores it in local storage, and then
 * redirects the user to the main application.
 *
 * <p>The redirect base URL defaults to {@code http://localhost:5173/auth/callback}
 * for local development and should be overridden via the
 * {@code app.oauth2.redirect-uri} property in production.
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LogManager.getLogger(OAuth2SuccessHandler.class);

    /** Query parameter name used to carry the JWT back to the React SPA. */
    private static final String TOKEN_PARAM = "token";

    /** Default redirect target — overridden in production via config. */
    private static final String DEFAULT_REDIRECT_URI = "http://localhost:5173/auth/callback";

    private final JwtTokenProvider tokenProvider;

    public OAuth2SuccessHandler(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
        // Disable the default saved-request mechanism; we always redirect to the SPA.
        setAlwaysUseDefaultTargetUrl(true);
        setDefaultTargetUrl(DEFAULT_REDIRECT_URI);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication)
            throws IOException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String token = tokenProvider.generateToken(user);
        log.debug("Generated JWT for user id={} email={}", user.getId(), user.getEmail());

        String redirectUri = UriComponentsBuilder
                .fromUriString(DEFAULT_REDIRECT_URI)
                .queryParam(TOKEN_PARAM, token)
                .build()
                .toUriString();

        log.info("OAuth2 login succeeded for email={}, redirecting to SPA", user.getEmail());

        // Clear any authentication attributes (e.g., state cookie) set during the OAuth flow.
        clearAuthenticationAttributes(request);

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
