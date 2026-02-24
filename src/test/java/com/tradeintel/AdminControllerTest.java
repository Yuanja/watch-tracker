package com.tradeintel;

import com.tradeintel.admin.AuditLogRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@link com.tradeintel.admin.AdminController}.
 *
 * <p>Covers user management, audit log, and WhatsApp group management endpoints
 * under {@code /api/admin/**}, all protected by {@code @UberAdminOnly}.</p>
 *
 * <p>Tests verify both the happy-path (uber_admin can access) and the access-control
 * boundary (admin user is denied with 403).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminControllerTest {

    private static final Logger log = LogManager.getLogger(AdminControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WhatsappGroupRepository groupRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TestDatabaseCleaner dbCleaner;

    /** Admin user — NOT uber_admin; should be denied all /api/admin endpoints. */
    private User adminUser;

    /** Uber-admin — should have full access to all /api/admin endpoints. */
    private User uberAdmin;

    /** A regular user to act as the target of role/active-toggle operations. */
    private User targetUser;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();

        adminUser  = TestHelper.createUser(userRepository, "admin@admin-test.com",      UserRole.admin);
        uberAdmin  = TestHelper.createUser(userRepository, "uber@admin-test.com",       UserRole.uber_admin);
        targetUser = TestHelper.createUser(userRepository, "target@admin-test.com",     UserRole.user);

        log.info("AdminControllerTest setUp complete: adminId={}, uberId={}, targetId={}",
                adminUser.getId(), uberAdmin.getId(), targetUser.getId());
    }

    /**
     * Tears down all data created in this test class in FK-safe order so that
     * subsequent test classes sharing the named H2 database are not affected by
     * leftover audit_log rows which reference users.
     */
    @AfterEach
    void tearDown() {
        dbCleaner.cleanAll();
    }

    // =========================================================================
    // GET /api/admin/users — list all users
    // =========================================================================

    @Test
    @DisplayName("GET /api/admin/users returns 200 with paginated user list for uber_admin")
    void listUsers_asUberAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content",       notNullValue()))
                // Three users were created in setUp: adminUser, uberAdmin, targetUser.
                .andExpect(jsonPath("$.totalElements", equalTo(3)));

        log.info("Verified GET /api/admin/users returns paginated user list for uber_admin");
    }

    @Test
    @DisplayName("GET /api/admin/users returns 403 when called by admin (not uber_admin)")
    void listUsers_asAdmin_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        log.info("Verified GET /api/admin/users returns 403 for admin (non-uber)");
    }

    // =========================================================================
    // PUT /api/admin/users/{id}/role — change user role
    // =========================================================================

    @Test
    @DisplayName("PUT /api/admin/users/{id}/role returns 200 and changes user role when uber_admin calls")
    void changeUserRole_asUberAdmin_returns200() throws Exception {
        String auth     = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);
        String targetId = targetUser.getId().toString();

        mockMvc.perform(put("/api/admin/users/{id}/role", targetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",   equalTo(targetId)))
                .andExpect(jsonPath("$.role", equalTo("admin")));

        log.info("Verified PUT /api/admin/users/{}/role changes role to admin", targetId);
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/role returns 403 when called by admin (not uber_admin)")
    void changeUserRole_asAdmin_returns403() throws Exception {
        String auth     = TestHelper.bearerHeader(jwtTokenProvider, adminUser);
        String targetId = targetUser.getId().toString();

        mockMvc.perform(put("/api/admin/users/{id}/role", targetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "admin"}
                                """))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // PUT /api/admin/users/{id}/active — toggle active status
    // =========================================================================

    @Test
    @DisplayName("PUT /api/admin/users/{id}/active returns 200 and disables user when uber_admin calls")
    void toggleUserActive_asUberAdmin_disablesUser() throws Exception {
        String auth     = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);
        String targetId = targetUser.getId().toString();

        mockMvc.perform(put("/api/admin/users/{id}/active", targetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",       equalTo(targetId)))
                .andExpect(jsonPath("$.isActive", equalTo(false)));

        log.info("Verified PUT /api/admin/users/{}/active sets isActive=false", targetId);
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/active returns 200 and re-enables user when uber_admin calls")
    void toggleUserActive_asUberAdmin_reEnablesUser() throws Exception {
        String auth     = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);
        String targetId = targetUser.getId().toString();

        // Disable first.
        mockMvc.perform(put("/api/admin/users/{id}/active", targetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}
                                """))
                .andExpect(status().isOk());

        // Re-enable.
        mockMvc.perform(put("/api/admin/users/{id}/active", targetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", equalTo(true)));

        log.info("Verified PUT /api/admin/users/{}/active re-enables user", targetId);
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/active returns 403 when called by admin (not uber_admin)")
    void toggleUserActive_asAdmin_returns403() throws Exception {
        String auth     = TestHelper.bearerHeader(jwtTokenProvider, adminUser);
        String targetId = targetUser.getId().toString();

        mockMvc.perform(put("/api/admin/users/{id}/active", targetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}
                                """))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/admin/audit — audit log
    // =========================================================================

    @Test
    @DisplayName("GET /api/admin/audit returns 200 with paginated audit log for uber_admin")
    void getAuditLog_asUberAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);

        mockMvc.perform(get("/api/admin/audit")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content",       notNullValue()))
                .andExpect(jsonPath("$.totalElements", notNullValue()));

        log.info("Verified GET /api/admin/audit returns paginated audit log for uber_admin");
    }

    @Test
    @DisplayName("GET /api/admin/audit returns 403 when called by admin (not uber_admin)")
    void getAuditLog_asAdmin_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(get("/api/admin/audit")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/admin/groups — create WhatsApp group
    // =========================================================================

    @Test
    @DisplayName("POST /api/admin/groups returns 200 and creates group when uber_admin calls")
    void createGroup_asUberAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);

        mockMvc.perform(post("/api/admin/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "whapiGroupId": "new-group-001@g.us",
                                  "groupName":    "Industrial Surplus Group",
                                  "description":  "Main trade group"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",           notNullValue()))
                .andExpect(jsonPath("$.whapiGroupId", equalTo("new-group-001@g.us")))
                .andExpect(jsonPath("$.groupName",    equalTo("Industrial Surplus Group")))
                .andExpect(jsonPath("$.isActive",     equalTo(true)));

        log.info("Verified POST /api/admin/groups creates a new WhatsApp group");
    }

    @Test
    @DisplayName("POST /api/admin/groups returns 403 when called by admin (not uber_admin)")
    void createGroup_asAdmin_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(post("/api/admin/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "whapiGroupId": "forbidden-group@g.us",
                                  "groupName":    "Forbidden Group"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/admin/groups — list WhatsApp groups
    // =========================================================================

    @Test
    @DisplayName("GET /api/admin/groups returns 200 with all groups for uber_admin")
    void listGroups_asUberAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);

        // Create two groups first.
        mockMvc.perform(post("/api/admin/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"whapiGroupId": "group-alpha@g.us", "groupName": "Alpha Group"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"whapiGroupId": "group-beta@g.us", "groupName": "Beta Group"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/groups")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        log.info("Verified GET /api/admin/groups returns list of two groups");
    }

    // =========================================================================
    // DELETE /api/admin/groups/{id} — deactivate group
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/admin/groups/{id} returns 204 and deactivates group when uber_admin calls")
    void deactivateGroup_asUberAdmin_returns204() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);

        // Create a group to deactivate.
        MvcResult createResult = mockMvc.perform(post("/api/admin/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"whapiGroupId": "group-to-remove@g.us", "groupName": "Remove Me"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the returned group ID from the JSON response body.
        String responseBody = createResult.getResponse().getContentAsString();
        // Extract the UUID from the "id" field — pattern: "id":"<uuid>"
        String groupId = responseBody.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(delete("/api/admin/groups/{id}", groupId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // The group should still appear in the list (retained for history) but as inactive.
        mockMvc.perform(get("/api/admin/groups")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive", equalTo(false)));

        log.info("Verified DELETE /api/admin/groups/{} deactivates the group", groupId);
    }

    @Test
    @DisplayName("DELETE /api/admin/groups/{id} returns 403 when called by admin (not uber_admin)")
    void deactivateGroup_asAdmin_returns403() throws Exception {
        String uberAuth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);
        String adminAuth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // First create a group as uber_admin.
        MvcResult createResult = mockMvc.perform(post("/api/admin/groups")
                        .header("Authorization", uberAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"whapiGroupId": "admin-forbidden-group@g.us", "groupName": "Admin Forbidden Group"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String groupId = responseBody.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Admin (not uber_admin) should be denied.
        mockMvc.perform(delete("/api/admin/groups/{id}", groupId)
                        .header("Authorization", adminAuth))
                .andExpect(status().isForbidden());
    }
}
