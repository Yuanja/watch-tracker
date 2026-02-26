package com.tradeintel.common.openai;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed request for the OpenAI Chat Completions API.
 *
 * <p>Encapsulates model, messages, temperature, and optional function/tool
 * definitions for the chat completion call.</p>
 */
public class ChatCompletionRequest {

    private final String model;
    private final List<OpenAIClient.ChatMessage> messages;
    private final double temperature;
    private final List<FunctionDefinition> functions;

    private ChatCompletionRequest(Builder builder) {
        this.model = builder.model;
        this.messages = List.copyOf(builder.messages);
        this.temperature = builder.temperature;
        this.functions = builder.functions.isEmpty()
                ? List.of()
                : List.copyOf(builder.functions);
    }

    public String getModel() { return model; }
    public List<OpenAIClient.ChatMessage> getMessages() { return messages; }
    public double getTemperature() { return temperature; }
    public List<FunctionDefinition> getFunctions() { return functions; }
    public boolean hasFunctions() { return !functions.isEmpty(); }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private final List<OpenAIClient.ChatMessage> messages = new ArrayList<>();
        private double temperature = 0.7;
        private final List<FunctionDefinition> functions = new ArrayList<>();

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder addMessage(String role, String content) {
            this.messages.add(new OpenAIClient.ChatMessage(role, content));
            return this;
        }

        public Builder messages(List<OpenAIClient.ChatMessage> messages) {
            this.messages.addAll(messages);
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder addFunction(FunctionDefinition function) {
            this.functions.add(function);
            return this;
        }

        public Builder functions(List<FunctionDefinition> functions) {
            this.functions.addAll(functions);
            return this;
        }

        public ChatCompletionRequest build() {
            if (model == null || model.isBlank()) {
                throw new IllegalStateException("model is required");
            }
            if (messages.isEmpty()) {
                throw new IllegalStateException("at least one message is required");
            }
            return new ChatCompletionRequest(this);
        }
    }
}
