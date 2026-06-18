package com.everycent.llm.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private Boolean enabled = true;

    private String provider = "openai-compatible";

    private String baseUrl = "";

    private String apiKey = "";

    private String model = "gpt-compatible-model";

    @Min(1)
    private Integer timeoutSeconds = 20;

    @DecimalMin("0.00")
    @DecimalMax("1.00")
    private Double minConfidence = 0.70;

    @Min(1)
    private Integer maxInputLength = 500;

    @Min(1)
    private Integer maxTokens = 180;

    @DecimalMin("0.00")
    @DecimalMax("2.00")
    private Double temperature = 0.75;

    @DecimalMin("0.00")
    @DecimalMax("1.00")
    private Double topP = 0.85;

    private Boolean randomizeSampling = false;

    @DecimalMin("0.00")
    @DecimalMax("2.00")
    private Double minTemperature = 0.75;

    @DecimalMin("0.00")
    @DecimalMax("2.00")
    private Double maxTemperature = 0.75;

    @DecimalMin("0.00")
    @DecimalMax("1.00")
    private Double minTopP = 0.85;

    @DecimalMin("0.00")
    @DecimalMax("1.00")
    private Double maxTopP = 0.85;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(Double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public Integer getMaxInputLength() {
        return maxInputLength;
    }

    public void setMaxInputLength(Integer maxInputLength) {
        this.maxInputLength = maxInputLength;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Boolean getRandomizeSampling() {
        return randomizeSampling;
    }

    public void setRandomizeSampling(Boolean randomizeSampling) {
        this.randomizeSampling = randomizeSampling;
    }

    public Double getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(Double minTemperature) {
        this.minTemperature = minTemperature;
    }

    public Double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(Double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public Double getMinTopP() {
        return minTopP;
    }

    public void setMinTopP(Double minTopP) {
        this.minTopP = minTopP;
    }

    public Double getMaxTopP() {
        return maxTopP;
    }

    public void setMaxTopP(Double maxTopP) {
        this.maxTopP = maxTopP;
    }
}
