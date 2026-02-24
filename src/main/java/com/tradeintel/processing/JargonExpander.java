package com.tradeintel.processing;

import com.tradeintel.common.entity.JargonEntry;
import com.tradeintel.normalize.JargonRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands known jargon acronyms in raw message text before LLM extraction.
 *
 * <p>Uses the verified jargon dictionary to perform case-insensitive,
 * word-boundary-aware replacement of acronyms with their expanded forms.
 * This pre-processing step improves LLM extraction accuracy by disambiguating
 * industry-specific abbreviations.</p>
 *
 * <p>Injects {@link JargonRepository} directly instead of {@link com.tradeintel.normalize.JargonService}
 * to avoid potential circular dependency issues when both services are involved
 * in the processing pipeline.</p>
 */
@Service
public class JargonExpander {

    private static final Logger log = LogManager.getLogger(JargonExpander.class);

    private final JargonRepository jargonRepository;

    public JargonExpander(JargonRepository jargonRepository) {
        this.jargonRepository = jargonRepository;
    }

    /**
     * Replaces known jargon acronyms in the given text with their expansions.
     *
     * <p>Each verified jargon entry's acronym is matched using word boundaries
     * ({@code \b}) so that partial matches within longer words are not replaced.
     * Replacement is case-insensitive. The original acronym is appended in
     * parentheses to preserve context, e.g. {@code "NOS"} becomes
     * {@code "New Old Stock (NOS)"}.</p>
     *
     * @param text the raw message text to expand
     * @return the text with known acronyms expanded, or the original text if
     *         it is null/blank or no jargon entries are found
     */
    @Transactional(readOnly = true)
    public String expand(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        List<JargonEntry> verified = jargonRepository.findByVerifiedTrueOrderByAcronymAsc();
        if (verified.isEmpty()) {
            log.debug("No verified jargon entries found; returning original text");
            return text;
        }

        // Build a map of acronym -> expansion
        Map<String, String> jargonMap = new HashMap<>();
        for (JargonEntry entry : verified) {
            jargonMap.put(entry.getAcronym().toLowerCase(), entry.getExpansion());
        }

        String result = text;
        int replacementCount = 0;

        for (Map.Entry<String, String> entry : jargonMap.entrySet()) {
            String acronym = entry.getKey();
            String expansion = entry.getValue();

            // Use word boundary regex for case-insensitive matching.
            // Pattern.quote ensures special regex characters in acronyms are escaped.
            Pattern pattern = Pattern.compile(
                    "\\b" + Pattern.quote(acronym) + "\\b",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                // Replace all occurrences, preserving the original acronym in parentheses
                // so the LLM sees both the expanded form and the original abbreviation.
                String replacement = expansion + " (" + matcher.group() + ")";
                result = matcher.replaceAll(Matcher.quoteReplacement(replacement));
                replacementCount++;
            }
        }

        if (replacementCount > 0) {
            log.debug("Expanded {} jargon terms in message text", replacementCount);
        }

        return result;
    }
}
