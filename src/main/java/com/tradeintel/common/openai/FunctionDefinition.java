package com.tradeintel.common.openai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a function/tool that can be provided to the OpenAI Chat Completions API
 * for function-calling.
 *
 * <p>Each definition includes a name, description, and a JSON Schema-like
 * parameter specification that tells the model what arguments the function accepts.</p>
 */
public record FunctionDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {

    public FunctionDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (parameters == null) {
            parameters = Map.of();
        }
    }

    /**
     * Builder for constructing a function definition with typed parameters.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private final Map<String, Object> properties = new LinkedHashMap<>();
        private final java.util.List<String> required = new java.util.ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addStringParam(String paramName, String paramDescription, boolean isRequired) {
            properties.put(paramName, Map.of("type", "string", "description", paramDescription));
            if (isRequired) required.add(paramName);
            return this;
        }

        public Builder addNumberParam(String paramName, String paramDescription, boolean isRequired) {
            properties.put(paramName, Map.of("type", "number", "description", paramDescription));
            if (isRequired) required.add(paramName);
            return this;
        }

        public Builder addBooleanParam(String paramName, String paramDescription, boolean isRequired) {
            properties.put(paramName, Map.of("type", "boolean", "description", paramDescription));
            if (isRequired) required.add(paramName);
            return this;
        }

        public FunctionDefinition build() {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", Map.copyOf(properties));
            if (!required.isEmpty()) {
                parameters.put("required", java.util.List.copyOf(required));
            }
            return new FunctionDefinition(name, description, Map.copyOf(parameters));
        }
    }
}
