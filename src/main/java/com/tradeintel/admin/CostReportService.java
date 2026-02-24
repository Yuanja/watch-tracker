package com.tradeintel.admin;

import com.tradeintel.admin.dto.UserCostSummaryDTO;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.UsageLedger;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service that aggregates OpenAI API usage costs from the {@code usage_ledger}
 * table for per-user summaries and platform-wide cost reports.
 *
 * <p>All methods are read-only transactions — they never mutate the ledger.
 * CSV generation produces a flat text payload that the controller streams
 * back as a file download with the appropriate response headers.
 */
@Service
public class CostReportService {

    private static final Logger log = LogManager.getLogger(CostReportService.class);

    private final UsageLedgerRepository usageLedgerRepository;
    private final UserRepository userRepository;

    public CostReportService(UsageLedgerRepository usageLedgerRepository,
                             UserRepository userRepository) {
        this.usageLedgerRepository = usageLedgerRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Per-user summary
    // -------------------------------------------------------------------------

    /**
     * Aggregates the usage ledger for a single user over the supplied date range
     * and returns a summary DTO. Throws {@link ResourceNotFoundException} if the
     * user does not exist.
     *
     * @param userId    the UUID of the user to summarise
     * @param startDate start of the reporting period (inclusive)
     * @param endDate   end of the reporting period (inclusive)
     * @return aggregated cost summary for the user
     * @throws ResourceNotFoundException if no user with the given id exists
     */
    @Transactional(readOnly = true)
    public UserCostSummaryDTO getUserCostSummary(UUID userId,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<UsageLedger> records =
                usageLedgerRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        return aggregate(user, records);
    }

    // -------------------------------------------------------------------------
    // All-users cost breakdown
    // -------------------------------------------------------------------------

    /**
     * Returns one {@link UserCostSummaryDTO} per user that has usage data within
     * the requested date range. Users with no records in the range are omitted.
     * The result list is ordered by total cost descending (highest spenders first).
     *
     * @param startDate start of the reporting period (inclusive)
     * @param endDate   end of the reporting period (inclusive)
     * @return list of per-user aggregated cost summaries, highest cost first
     */
    @Transactional(readOnly = true)
    public List<UserCostSummaryDTO> getAllUsersCosts(LocalDate startDate, LocalDate endDate) {
        List<UsageLedger> allRecords =
                usageLedgerRepository.findAllByDateRange(startDate, endDate);

        // Group records by user — LinkedHashMap preserves insertion order from
        // the repository query which is already sorted by user id then date.
        Map<UUID, List<UsageLedger>> byUser = new LinkedHashMap<>();
        Map<UUID, User> userMap = new LinkedHashMap<>();

        for (UsageLedger record : allRecords) {
            UUID uid = record.getUser().getId();
            byUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(record);
            userMap.putIfAbsent(uid, record.getUser());
        }

        List<UserCostSummaryDTO> summaries = new ArrayList<>();
        for (Map.Entry<UUID, List<UsageLedger>> entry : byUser.entrySet()) {
            User user = userMap.get(entry.getKey());
            summaries.add(aggregate(user, entry.getValue()));
        }

        // Sort by total cost descending
        summaries.sort((a, b) -> b.totalCostUsd().compareTo(a.totalCostUsd()));

        log.debug("Cost report: {} users with data between {} and {}",
                summaries.size(), startDate, endDate);

        return summaries;
    }

    // -------------------------------------------------------------------------
    // CSV export
    // -------------------------------------------------------------------------

    /**
     * Generates a UTF-8 CSV string of all per-user costs within the requested
     * date range. The controller streams this back with
     * {@code Content-Type: text/csv} and a {@code Content-Disposition: attachment}
     * header so that the browser offers it as a file download.
     *
     * <p>Columns: user_id, email, display_name, total_input_tokens,
     * total_output_tokens, total_cost_usd, total_sessions, record_count,
     * start_date, end_date.
     *
     * @param startDate start of the reporting period (inclusive)
     * @param endDate   end of the reporting period (inclusive)
     * @return CSV content as a plain string (UTF-8)
     */
    @Transactional(readOnly = true)
    public String exportCostsCsv(LocalDate startDate, LocalDate endDate) {
        List<UserCostSummaryDTO> summaries = getAllUsersCosts(startDate, endDate);

        StringBuilder csv = new StringBuilder();
        csv.append("user_id,email,display_name,total_input_tokens,total_output_tokens,")
           .append("total_cost_usd,total_sessions,record_count,start_date,end_date\n");

        for (UserCostSummaryDTO s : summaries) {
            csv.append(s.userId()).append(',')
               .append(escapeCsvField(s.userEmail())).append(',')
               .append(escapeCsvField(s.displayName())).append(',')
               .append(s.totalInputTokens()).append(',')
               .append(s.totalOutputTokens()).append(',')
               .append(s.totalCostUsd().toPlainString()).append(',')
               .append(s.totalSessions()).append(',')
               .append(s.recordCount()).append(',')
               .append(startDate).append(',')
               .append(endDate).append('\n');
        }

        return csv.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Aggregates a list of daily usage ledger entries into a single summary DTO.
     *
     * @param user    the owning user entity
     * @param records daily ledger rows for this user
     * @return aggregated summary
     */
    private UserCostSummaryDTO aggregate(User user, List<UsageLedger> records) {
        long totalInputTokens = 0L;
        long totalOutputTokens = 0L;
        BigDecimal totalCostUsd = BigDecimal.ZERO;
        int totalSessions = 0;

        for (UsageLedger record : records) {
            totalInputTokens += record.getTotalInputTokens() != null
                    ? record.getTotalInputTokens() : 0L;
            totalOutputTokens += record.getTotalOutputTokens() != null
                    ? record.getTotalOutputTokens() : 0L;
            totalCostUsd = totalCostUsd.add(
                    record.getTotalCostUsd() != null ? record.getTotalCostUsd() : BigDecimal.ZERO);
            totalSessions += record.getSessionCount() != null ? record.getSessionCount() : 0;
        }

        return new UserCostSummaryDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                totalInputTokens,
                totalOutputTokens,
                totalCostUsd,
                totalSessions,
                records.size()
        );
    }

    /**
     * Wraps a CSV field value in double-quotes when it contains commas, newlines,
     * or double-quote characters. Embedded double-quotes are escaped by doubling.
     *
     * @param value the raw field value; may be {@code null}
     * @return RFC 4180 compliant CSV field value
     */
    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
