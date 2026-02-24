package com.tradeintel;

import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@link com.tradeintel.auth.AuthController}.
 *
 * <p>Tests run against the full Spring Boot application context with an H2
 * in-memory database.  Schema is created via {@code ddl-auto: create-drop}
 * and Flyway is disabled in the test {@code application.yml}.</p>
 *
 * <p>{@link DirtiesContext} ensures a clean context between test classes to
 * prevent H2 schema conflicts when multiple test classes run in the same JVM.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerTest {

    private static final Logger log = LogManager.getLogger(AuthControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up any leftover users from previous tests in this class.
        userRepository.deleteAll();

        testUser = TestHelper.createUser(userRepository, "testuser@example.com", UserRole.user);
        log.info("Set up test user id={}", testUser.getId());
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me — authenticated
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/auth/me returns 200 with user profile when JWT is valid")
    void getMe_authenticated_returns200WithProfile() throws Exception {
        String authHeader = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", equalTo("testuser@example.com")))
                .andExpect(jsonPath("$.displayName", equalTo("Test User testuser@example.com")))
                .andExpect(jsonPath("$.role", equalTo("user")));

        log.info("Verified GET /api/auth/me returns profile for authenticated user");
    }

    @Test
    @DisplayName("GET /api/auth/me returns 200 with admin role when user is admin")
    void getMe_adminUser_returnsAdminRole() throws Exception {
        User adminUser = TestHelper.createUser(userRepository, "admin@example.com", UserRole.admin);
        String authHeader = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", equalTo("admin@example.com")))
                .andExpect(jsonPath("$.role", equalTo("admin")));
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me — unauthenticated
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/auth/me returns 401 when no Authorization header is present")
    void getMe_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        log.info("Verified GET /api/auth/me returns 401 for unauthenticated request");
    }

    @Test
    @DisplayName("GET /api/auth/me returns 401 when Authorization header contains garbage")
    void getMe_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer this-is-not-a-valid-jwt")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/me returns 401 when token is for a non-existent user")
    void getMe_tokenForDeletedUser_returns401() throws Exception {
        // Generate a valid token for the user, then delete the user from DB.
        String authHeader = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        userRepository.deleteAll();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
