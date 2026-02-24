package com.tradeintel.archive;

import com.tradeintel.common.openai.OpenAIClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private static final Logger log = LogManager.getLogger(EmbeddingService.class);

    private final OpenAIClient openAIClient;
    private final String embeddingModel;

    public EmbeddingService(OpenAIClient openAIClient,
                           @Value("${app.openai.embedding-model}") String embeddingModel) {
        this.openAIClient = openAIClient;
        this.embeddingModel = embeddingModel;
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // Truncate to ~8000 tokens worth of text (rough estimate)
        String truncated = text.length() > 30000 ? text.substring(0, 30000) : text;
        return openAIClient.embed(truncated, embeddingModel);
    }
}
