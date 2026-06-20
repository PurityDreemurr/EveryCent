package com.everycent.assistant.skill;

import java.util.LinkedHashMap;
import java.util.Map;

public class AssistantAction {

    private String name;

    private Boolean requiresConfirmation = false;

    private Integer priority = 0;

    private Map<String, Object> arguments = new LinkedHashMap<>();

    public AssistantAction() {}

    public AssistantAction(String name) {
        this.name = name;
    }

    public AssistantAction(String name, Map<String, Object> arguments) {
        this.name = name;
        setArguments(arguments);
    }

    public boolean isAllowed() {
        return AssistantActionCatalog.isAllowed(name);
    }

    public boolean isForbidden() {
        return AssistantActionCatalog.isForbidden(name);
    }

    public RiskLevel riskLevel() {
        return AssistantActionCatalog.riskLevel(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(Boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? new LinkedHashMap<>() : arguments;
    }
}
