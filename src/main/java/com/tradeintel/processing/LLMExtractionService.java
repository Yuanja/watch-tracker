package com.tradeintel.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeintel.common.openai.OpenAIClient;
import com.tradeintel.common.openai.OpenAIClient.ChatCompletionResponse;
import com.tradeintel.common.openai.OpenAIClient.ChatMessage;
import com.tradeintel.normalize.CategoryService;
import com.tradeintel.normalize.JargonService;
import com.tradeintel.normalize.ManufacturerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls the OpenAI API to extract structured trade listing data from expanded
 * WhatsApp message text.
 *
 * <p>The extraction prompt template is loaded from {@code classpath:prompts/extraction.txt}
 * at construction time. At extraction time the template is populated with the current
 * admin-managed categories, manufacturers, and verified jargon CSV values so that
 * the LLM can match extracted fields to known normalised values.</p>
 *
 * <p>Uses the configured extraction model (default {@code gpt-4o-mini}) at low
 * temperature (0.1) for deterministic, structured output.</p>
 */
@Service
public class LLMExtractionService {

    private static final Logger log = LogManager.getLogger(LLMExtractionService.class);

    private final OpenAIClient openAIClient;
    private final CategoryService categoryService;
    private final ManufacturerService manufacturerService;
    private final JargonService jargonService;
    private final ObjectMapper objectMapper;
    private final String extractionModel;
    private final String promptTemplate;

    public LLMExtractionService(OpenAIClient openAIClient,
                                CategoryService categoryService,
                                ManufacturerService manufacturerService,
                                JargonService jargonService,
                                @Value("${app.openai.extraction-model}") String extractionModel) {
        this.openAIClient = openAIClient;
        this.categoryService = categoryService;
        this.manufacturerService = manufacturerService;
        this.jargonService = jargonService;
        this.objectMapper = new ObjectMapper();
        this.extractionModel = extractionModel;
        this.promptTemplate = loadPromptTemplate();
    }

    /**
     * Extracts structured listing data from the given message text.
     *
     * @param expandedText  the jargon-expanded message text
     * @param originalText  the original, unexpanded message text (not currently used
     *                      in the prompt but available for future enhancements)
     * @return the extraction result containing intent, items, unknown terms, and confidence
     */
    public ExtractionResult extract(String expandedText, String originalText) {
        if (expandedText == null || expandedText.isBlank()) {
            log.warn("Empty text provided for extraction; returning unknown result");
            return buildFallbackResult();
        }

        try {
            String categoriesCSV = categoryService.getAllNamesAsCSV();
            String manufacturersCSV = manufacturerService.getAllNamesWithAliasesAsCSV();
            String jargonCSV = jargonService.getVerifiedAsCSV();

            String formattedPrompt = String.format(
                    promptTemplate,
                    categoriesCSV,
                    manufacturersCSV,
                    jargonCSV,
                    expandedText
            );

            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", formattedPrompt)
            );

            ChatCompletionResponse response = openAIClient.chatCompletion(
                    extractionModel, messages, 0.1
            );

            String content = response.content();
            log.info("LLM extraction: model={}, inputTokens={}, outputTokens={}, estimatedCost={}",
                    extractionModel, response.inputTokens(), response.outputTokens(),
                    String.format("$%.6f", response.estimateCost()));
            log.debug("LLM extraction response content: {}",
                    content.length() > 200 ? content.substring(0, 200) + "..." : content);

            return parseResponse(content);

        } catch (Exception e) {
            log.error("LLM extraction failed for text: {}",
                    expandedText.length() > 100 ? expandedText.substring(0, 100) + "..." : expandedText, e);
            return buildFallbackResult();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Parses the LLM JSON response into an {@link ExtractionResult}.
     * Handles the common case where the LLM wraps the JSON in a markdown code fence.
     */
    private ExtractionResult parseResponse(String content) {
        try {
            // Strip markdown code fences if present (```json ... ```)
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                if (firstNewline > 0) {
                    cleaned = cleaned.substring(firstNewline + 1);
                }
                if (cleaned.endsWith("```")) {
                    cleaned = cleaned.substring(0, cleaned.length() - 3);
                }
                cleaned = cleaned.trim();
            }

            return objectMapper.readValue(cleaned, ExtractionResult.class);

        } catch (Exception e) {
            log.warn("Failed to parse LLM extraction response as JSON: {}", e.getMessage());
            return buildFallbackResult();
        }
    }

    /**
     * Returns a safe fallback result with unknown intent and zero confidence
     * when extraction fails.
     */
    private ExtractionResult buildFallbackResult() {
        ExtractionResult result = new ExtractionResult();
        result.setIntent("unknown");
        result.setItems(new ArrayList<>());
        result.setUnknownTerms(new ArrayList<>());
        result.setConfidence(0.0);
        return result;
    }

    /**
     * Loads the extraction prompt template from the classpath at construction time.
     */
    private String loadPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/extraction.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load extraction prompt template from classpath", e);
            throw new IllegalStateException("Could not load prompts/extraction.txt", e);
        }
    }
}
