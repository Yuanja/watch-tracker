package com.tradeintel.common.openai;

/**
 * Typed request for the OpenAI Embeddings API.
 *
 * <p>Encapsulates the input text and model to be sent to the
 * {@code /v1/embeddings} endpoint.</p>
 */
public record EmbeddingRequest(String model, String input) {

    public EmbeddingRequest {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model is required");
        }
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input is required");
        }
    }

    /**
     * Creates an embedding request with the given model and input text,
     * truncating the input to the maximum safe length if necessary.
     *
     * @param model    the OpenAI embedding model name
     * @param input    the text to embed
     * @param maxChars maximum character length for the input (0 = no limit)
     * @return a new EmbeddingRequest
     */
    public static EmbeddingRequest of(String model, String input, int maxChars) {
        String truncated = (maxChars > 0 && input.length() > maxChars)
                ? input.substring(0, maxChars)
                : input;
        return new EmbeddingRequest(model, truncated);
    }
}
