package com.tradeintel;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility component that deletes all rows from every table in FK-safe order.
 *
 * <p>Shared across integration test classes to avoid FK constraint violations
 * when tests running in different Spring contexts (e.g. those that use
 * {@code @MockBean}) share the same named H2 database.</p>
 */
@Component
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbc;

    public TestDatabaseCleaner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Deletes all rows from every table in FK-safe (reverse-dependency) order.
     * Call this at the start of each test's {@code @BeforeEach} to guarantee
     * a clean database regardless of what previous test classes may have left.
     */
    @Transactional
    public void cleanAll() {
        // Child tables first, then parent tables
        jdbc.execute("DELETE FROM chat_messages");
        jdbc.execute("DELETE FROM chat_sessions");
        jdbc.execute("DELETE FROM usage_ledger");
        jdbc.execute("DELETE FROM audit_log");
        jdbc.execute("DELETE FROM notification_rules");
        jdbc.execute("DELETE FROM review_queue");
        jdbc.execute("DELETE FROM listings");
        jdbc.execute("DELETE FROM raw_messages");
        jdbc.execute("DELETE FROM jargon_dictionary");
        jdbc.execute("DELETE FROM categories");
        jdbc.execute("DELETE FROM manufacturers");
        jdbc.execute("DELETE FROM units");
        jdbc.execute("DELETE FROM conditions");
        jdbc.execute("DELETE FROM whatsapp_groups");
        jdbc.execute("DELETE FROM users");
    }
}
