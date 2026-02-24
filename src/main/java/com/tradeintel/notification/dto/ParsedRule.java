package com.tradeintel.notification.dto;

import java.util.List;

/**
 * Structured result from parsing a natural language notification rule via the LLM.
 *
 * @param intent        the parsed intent filter ("sell", "want", or null for any)
 * @param keywords      keywords extracted from the rule for fuzzy matching
 * @param categoryNames category names mentioned in the rule
 * @param priceMin      minimum price filter, or null
 * @param priceMax      maximum price filter, or null
 */
public record ParsedRule(
        String intent,
        List<String> keywords,
        List<String> categoryNames,
        Double priceMin,
        Double priceMax
) {}
