package com.everycent.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.vector-store")
public class VectorStoreProperties {

    private String provider = "qdrant";

    private String baseUrl = "http://127.0.0.1:6333";

    private String memoryCollection = "everycent_ai_memory";

    private String roleKnowledgeCollection = "everycent_ai_role_knowledge";

    @Min(1)
    private Integer timeoutSeconds = 10;

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

    public String getMemoryCollection() {
        return memoryCollection;
    }

    public void setMemoryCollection(String memoryCollection) {
        this.memoryCollection = memoryCollection;
    }

    public String getRoleKnowledgeCollection() {
        return roleKnowledgeCollection;
    }

    public void setRoleKnowledgeCollection(String roleKnowledgeCollection) {
        this.roleKnowledgeCollection = roleKnowledgeCollection;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
