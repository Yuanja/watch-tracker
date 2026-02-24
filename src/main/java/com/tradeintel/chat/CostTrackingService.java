package com.tradeintel.chat;

import com.tradeintel.admin.UsageLedgerRepository;
import com.tradeintel.common.entity.UsageLedger;
import com.tradeintel.common.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for tracking OpenAI API usage costs per user per day.
 *
 * <p>Finds or creates a {@link UsageLedger} entry for the (user, today) pair
 * and atomically increments the token counts and cost.</p>
 */
@Service
public class CostTrackingService {

    private static final Logger log = LogManager.getLogger(CostTrackingService.class);

    private final UsageLedgerRepository usageLedgerRepository;

    public CostTrackingService(UsageLedgerRepository usageLedgerRepository) {
        this.usageLedgerRepository = usageLedgerRepository;
    }

    /**
     * Tracks a single LLM call's usage for the given user.
     *
     * @param user         the user who triggered the LLM call
     * @param model        the OpenAI model name (for logging)
     * @param inputTokens  number of input tokens used
     * @param outputTokens number of output tokens used
     * @param costUsd      estimated cost in USD
     */
    @Transactional
    public void trackUsage(User user, String model, int inputTokens, int outputTokens, double costUsd) {
        LocalDate today = LocalDate.now();

        UsageLedger ledger = findOrCreate(user, today);

        ledger.setTotalInputTokens(ledger.getTotalInputTokens() + inputTokens);
        ledger.setTotalOutputTokens(ledger.getTotalOutputTokens() + outputTokens);
        ledger.setTotalCostUsd(ledger.getTotalCostUsd().add(BigDecimal.valueOf(costUsd)));

        usageLedgerRepository.save(ledger);

        log.debug("Tracked usage for user={}: model={}, inputTokens={}, outputTokens={}, cost={}",
                user.getId(), model, inputTokens, outputTokens, costUsd);
    }

    /**
     * Increments the session count for the user on the current day.
     *
     * @param user the user who started a new session
     */
    @Transactional
    public void incrementSessionCount(User user) {
        LocalDate today = LocalDate.now();
        UsageLedger ledger = findOrCreate(user, today);
        ledger.setSessionCount(ledger.getSessionCount() + 1);
        usageLedgerRepository.save(ledger);
    }

    /**
     * Finds the existing ledger entry for the given user and date, or creates
     * a new one if none exists.
     */
    private UsageLedger findOrCreate(User user, LocalDate date) {
        // Use date range query to find by user + date
        List<UsageLedger> entries = usageLedgerRepository.findByUserIdAndDateRange(
                user.getId(), date, date);

        if (!entries.isEmpty()) {
            return entries.get(0);
        }

        UsageLedger newLedger = new UsageLedger();
        newLedger.setUser(user);
        newLedger.setPeriodDate(date);
        newLedger.setTotalInputTokens(0L);
        newLedger.setTotalOutputTokens(0L);
        newLedger.setTotalCostUsd(BigDecimal.ZERO);
        newLedger.setSessionCount(0);
        return usageLedgerRepository.save(newLedger);
    }
}
