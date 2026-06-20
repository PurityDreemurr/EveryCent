package com.everycent.assistant.skill;

public interface Skill {
    String name();

    String description();

    boolean supports(String actionName);

    SkillResult execute(AssistantAction action, SkillExecutionContext context);
}
