package com.tradeintel.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeintel.common.openai.OpenAIClient;
import com.tradeintel.notification.dto.ParsedRule;
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
 * Parses a natural language notification rule into structured filter criteria
 * using the OpenAI LLM.
 *
 * <p>Reads the prompt template from {@code resources/prompts/rule_parser.txt},
 * substitutes the user's rule text, and calls the extraction model. The JSON
 * response is parsed into a {@link ParsedRule} record.</p>
 */
@Service
public class NLRuleParser {

    private static final Logger log = LogManager.getLogger(NLRuleParser.class);

    private final OpenAIClient openAIClient;
    private final String extractionModel;
    private final String promptTemplate;

    public NLRuleParser(OpenAIClient openAIClient,
                        @Value("${app.openai.extraction-model}") String extractionModel) {
        this.openAIClient = openAIClient;
        this.extractionModel = extractionModel;
        this.promptTemplate = loadPromptTemplate();
    }

    /**
     * Parses a natural language notification rule into structured fields.
     *
     * @param nlRule the user's natural language rule description
     * @return the parsed structured rule
     */
    public ParsedRule parse(String nlRule) {
        String prompt = promptTemplate.replace("%s", nlRule);

        List<OpenAIClient.ChatMessage> messages = List.of(
                new OpenAIClient.ChatMessage("user", prompt)
        );

        log.info("Parsing notification rule via LLM: model={}, ruleText='{}'", extractionModel, nlRule);

        OpenAIClient.ChatCompletionResponse response = openAIClient.chatCompletion(
                extractionModel, messages, 0.1);

        return parseResponse(response.content());
    }

    private ParsedRule parseResponse(String content) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Extract JSON from the response (handle markdown code blocks)
            String jsonContent = content.trim();
            if (jsonContent.startsWith("```")) {
                int start = jsonContent.indexOf('{');
                int end = jsonContent.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    jsonContent = jsonContent.substring(start, end + 1);
                }
            }

            JsonNode root = mapper.readTree(jsonContent);

            String intent = root.has("intent") && !root.get("intent").isNull()
                    ? root.get("intent").asText()
                    : null;

            List<String> keywords = new ArrayList<>();
            if (root.has("keywords") && root.get("keywords").isArray()) {
                for (JsonNode kw : root.get("keywords")) {
                    keywords.add(kw.asText());
                }
            }

            List<String> categoryNames = new ArrayList<>();
            if (root.has("category_names") && root.get("category_names").isArray()) {
                for (JsonNode cn : root.get("category_names")) {
                    categoryNames.add(cn.asText());
                }
            }

            Double priceMin = root.has("price_min") && !root.get("price_min").isNull()
                    ? root.get("price_min").asDouble()
                    : null;

            Double priceMax = root.has("price_max") && !root.get("price_max").isNull()
                    ? root.get("price_max").asDouble()
                    : null;

            log.info("Parsed rule: intent={}, keywords={}, categories={}, priceMin={}, priceMax={}",
                    intent, keywords, categoryNames, priceMin, priceMax);

            return new ParsedRule(intent, keywords, categoryNames, priceMin, priceMax);

        } catch (Exception e) {
            log.error("Failed to parse NL rule response: {}", content, e);
            // Return a minimal parsed rule with the raw text as a keyword
            return new ParsedRule(null, List.of(), List.of(), null, null);
        }
    }

    private String loadPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/rule_parser.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load rule_parser.txt prompt template", e);
            return "Parse the following notification rule into JSON: \"%s\"";
        }
    }
}
