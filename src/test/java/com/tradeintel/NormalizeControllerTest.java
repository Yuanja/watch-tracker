package com.tradeintel;

import com.tradeintel.admin.AuditLogRepository;
import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.normalize.CategoryRepository;
import com.tradeintel.normalize.ManufacturerRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the normalize controller endpoints:
 * {@code /api/normalize/categories} and {@code /api/normalize/manufacturers}.
 *
 * <p>All tests run against the full Spring Boot context with an H2 in-memory
 * database. Schema is created from {@code h2-schema.sql}; Flyway is disabled.</p>
 *
 * <p>{@link DirtiesContext} at {@code AFTER_CLASS} level resets the Spring
 * context between test classes so H2 state does not leak across classes.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NormalizeControllerTest {

    private static final Logger log = LogManager.getLogger(NormalizeControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TestDatabaseCleaner dbCleaner;

    /** Regular user — should be denied access to normalize endpoints. */
    private User regularUser;

    /** Admin user — may access normalize CRUD. */
    private User adminUser;

    /** Uber-admin user — may also access normalize CRUD. */
    private User uberAdmin;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();

        regularUser = TestHelper.createUser(userRepository, "regular@example.com", UserRole.user);
        adminUser   = TestHelper.createUser(userRepository, "admin@example.com",   UserRole.admin);
        uberAdmin   = TestHelper.createUser(userRepository, "uber@example.com",    UserRole.uber_admin);

        log.info("NormalizeControllerTest setUp complete: user={}, admin={}, uber={}",
                regularUser.getId(), adminUser.getId(), uberAdmin.getId());
    }

    // =========================================================================
    // POST /api/normalize/categories — create
    // =========================================================================

    @Test
    @DisplayName("POST /api/normalize/categories returns 201 when admin creates a category")
    void createCategory_asAdmin_returns201() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Valves", "sortOrder": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",        notNullValue()))
                .andExpect(jsonPath("$.name",      equalTo("Valves")))
                .andExpect(jsonPath("$.isActive",  equalTo(true)))
                .andExpect(header().exists("Location"));

        log.info("Verified POST /api/normalize/categories returns 201 for admin");
    }

    @Test
    @DisplayName("POST /api/normalize/categories returns 201 when uber_admin creates a category")
    void createCategory_asUberAdmin_returns201() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);

        mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bearings"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Bearings")));
    }

    // =========================================================================
    // GET /api/normalize/categories — list
    // =========================================================================

    @Test
    @DisplayName("GET /api/normalize/categories returns 200 with active categories when admin requests")
    void listCategories_asAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Seed a category first.
        mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Pumps"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/normalize/categories")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",       notNullValue()))
                .andExpect(jsonPath("$",       hasSize(1)))
                .andExpect(jsonPath("$[0].name", equalTo("Pumps")));

        log.info("Verified GET /api/normalize/categories returns active category list");
    }

    @Test
    @DisplayName("GET /api/normalize/categories?activeOnly=false returns all categories including inactive ones")
    void listCategories_activeOnlyFalse_returnsAll() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Create and then deactivate a category.
        MvcResult createResult = mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hoses"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        // Parse the created ID from the Location header.
        String location = createResult.getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        // Deactivate it.
        mockMvc.perform(delete("/api/normalize/categories/{id}", id)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // activeOnly=true (default) should return empty.
        mockMvc.perform(get("/api/normalize/categories")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // activeOnly=false should include the deactivated entry.
        mockMvc.perform(get("/api/normalize/categories")
                        .param("activeOnly", "false")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // =========================================================================
    // PUT /api/normalize/categories/{id} — update
    // =========================================================================

    @Test
    @DisplayName("PUT /api/normalize/categories/{id} returns 200 with updated name when admin updates")
    void updateCategory_asAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Create a category and capture its ID.
        MvcResult createResult = mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Pipes"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        // Update name.
        mockMvc.perform(put("/api/normalize/categories/{id}", id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Pipes & Tubing", "sortOrder": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",        equalTo(id)))
                .andExpect(jsonPath("$.name",      equalTo("Pipes & Tubing")))
                .andExpect(jsonPath("$.sortOrder", equalTo(5)));

        log.info("Verified PUT /api/normalize/categories/{} updates name successfully", id);
    }

    // =========================================================================
    // DELETE /api/normalize/categories/{id} — deactivate (soft delete)
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/normalize/categories/{id} returns 204 and deactivates the category")
    void deactivateCategory_asAdmin_returns204() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        MvcResult createResult = mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Seals"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(delete("/api/normalize/categories/{id}", id)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // After deactivation the default list (activeOnly=true) should be empty.
        mockMvc.perform(get("/api/normalize/categories")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        log.info("Verified DELETE /api/normalize/categories/{} deactivates category", id);
    }

    // =========================================================================
    // Access control — regular user is denied
    // =========================================================================

    @Test
    @DisplayName("POST /api/normalize/categories returns 403 when called by a regular user")
    void createCategory_asRegularUser_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Forbidden Category"}
                                """))
                .andExpect(status().isForbidden());

        log.info("Verified regular user is denied POST /api/normalize/categories with 403");
    }

    @Test
    @DisplayName("GET /api/normalize/categories returns 403 when called by a regular user")
    void listCategories_asRegularUser_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(get("/api/normalize/categories")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Duplicate name returns 400
    // =========================================================================

    @Test
    @DisplayName("POST /api/normalize/categories returns 400 when category name already exists")
    void createCategory_duplicateName_returns400() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Create first.
        mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unique Category"}
                                """))
                .andExpect(status().isCreated());

        // Attempt to create with the same name.
        mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unique Category"}
                                """))
                .andExpect(status().isBadRequest());

        log.info("Verified duplicate category name returns 400");
    }

    // =========================================================================
    // POST /api/normalize/manufacturers — create
    // =========================================================================

    @Test
    @DisplayName("POST /api/normalize/manufacturers returns 201 when admin creates a manufacturer")
    void createManufacturer_asAdmin_returns201() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(post("/api/normalize/manufacturers")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Parker Hannifin", "website": "https://parker.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",       notNullValue()))
                .andExpect(jsonPath("$.name",     equalTo("Parker Hannifin")))
                .andExpect(jsonPath("$.website",  equalTo("https://parker.com")))
                .andExpect(jsonPath("$.isActive", equalTo(true)))
                .andExpect(header().exists("Location"));

        log.info("Verified POST /api/normalize/manufacturers returns 201 for admin");
    }

    // =========================================================================
    // GET /api/normalize/manufacturers — list
    // =========================================================================

    @Test
    @DisplayName("GET /api/normalize/manufacturers returns 200 with manufacturer list when admin requests")
    void listManufacturers_asAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(post("/api/normalize/manufacturers")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Emerson Electric"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/normalize/manufacturers")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "ABB Group"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/normalize/manufacturers")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        log.info("Verified GET /api/normalize/manufacturers returns list of two manufacturers");
    }

    @Test
    @DisplayName("POST /api/normalize/manufacturers returns 403 when called by a regular user")
    void createManufacturer_asRegularUser_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(post("/api/normalize/manufacturers")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Forbidden Manufacturer"}
                                """))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Audit logging — verify normalize CRUD operations create audit entries
    // =========================================================================

    @Test
    @DisplayName("Category create/update/deactivate operations generate audit log entries")
    void categoryCrud_generatesAuditLogEntries() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);
        long auditCountBefore = auditLogRepository.count();

        // Create
        MvcResult createResult = mockMvc.perform(post("/api/normalize/categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Audit Test Category"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        // Update
        mockMvc.perform(put("/api/normalize/categories/{id}", id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Audit Test Category Updated"}
                                """))
                .andExpect(status().isOk());

        // Deactivate
        mockMvc.perform(delete("/api/normalize/categories/{id}", id)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // 3 audit entries should have been generated (create + update + deactivate)
        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter - auditCountBefore).isGreaterThanOrEqualTo(3);
        log.info("Verified category CRUD generated {} audit log entries", auditCountAfter - auditCountBefore);
    }
}
