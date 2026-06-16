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
}
