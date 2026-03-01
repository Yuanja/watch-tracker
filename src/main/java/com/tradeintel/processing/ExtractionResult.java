package com.tradeintel.processing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO representing the structured output from the LLM extraction of a WhatsApp
 * trade message.
 *
 * <p>The JSON shape matches the schema defined in {@code resources/prompts/extraction.txt}
 * and is deserialized by Jackson after the OpenAI API call returns.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractionResult {

    private String intent;
    private List<ExtractedItem> items = new ArrayList<>();

    @JsonProperty("unknown_terms")
    private List<String> unknownTerms = new ArrayList<>();

    private double confidence;

    // Cost tracking (not part of LLM JSON response — set by LLMExtractionService)
    private transient int inputTokens;
    private transient int outputTokens;
    private transient double estimatedCostUsd;
    private transient String modelUsed;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ExtractionResult() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<ExtractedItem> getItems() {
        return items;
    }

    public void setItems(List<ExtractedItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public List<String> getUnknownTerms() {
        return unknownTerms;
    }

    public void setUnknownTerms(List<String> unknownTerms) {
        this.unknownTerms = unknownTerms != null ? unknownTerms : new ArrayList<>();
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public double getEstimatedCostUsd() { return estimatedCostUsd; }
    public void setEstimatedCostUsd(double estimatedCostUsd) { this.estimatedCostUsd = estimatedCostUsd; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    // -------------------------------------------------------------------------
    // Inner class: ExtractedItem
    // -------------------------------------------------------------------------

    /**
     * A single item extracted from a trade message. One message may contain
     * multiple items (e.g. "Selling 10x valves and 5x pumps").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractedItem {

        private String description;
        private String category;
        private String manufacturer;

        @JsonProperty("part_number")
        private String partNumber;

        @JsonProperty("model_name")
        private String modelName;

        @JsonProperty("dial_color")
        private String dialColor;

        @JsonProperty("case_material")
        private String caseMaterial;

        private Integer year;

        @JsonProperty("case_size_mm")
        private Integer caseSizeMm;

        @JsonProperty("set_composition")
        private String setComposition;

        @JsonProperty("bracelet_strap")
        private String braceletStrap;

        private Double quantity;
        private String unit;
        private Double price;
        private String currency;
        private String condition;

        public ExtractedItem() {
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getPartNumber() {
            return partNumber;
        }

        public void setPartNumber(String partNumber) {
            this.partNumber = partNumber;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getDialColor() {
            return dialColor;
        }

        public void setDialColor(String dialColor) {
            this.dialColor = dialColor;
        }

        public String getCaseMaterial() {
            return caseMaterial;
        }

        public void setCaseMaterial(String caseMaterial) {
            this.caseMaterial = caseMaterial;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public Integer getCaseSizeMm() {
            return caseSizeMm;
        }

        public void setCaseSizeMm(Integer caseSizeMm) {
            this.caseSizeMm = caseSizeMm;
        }

        public String getSetComposition() {
            return setComposition;
        }

        public void setSetComposition(String setComposition) {
            this.setComposition = setComposition;
        }

        public String getBraceletStrap() {
            return braceletStrap;
        }

        public void setBraceletStrap(String braceletStrap) {
            this.braceletStrap = braceletStrap;
        }
    }
}
