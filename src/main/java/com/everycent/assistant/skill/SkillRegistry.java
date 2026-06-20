package com.everycent.assistant.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class SkillRegistry {

    private final List<Skill> skills = new ArrayList<>();

    public SkillRegistry(Collection<Skill> skills) {
        if (skills != null) {
            this.skills.addAll(skills);
        }
    }

    public Optional<Skill> findSkill(AssistantAction action) {
        if (action == null) {
            return Optional.empty();
        }
        return findSkill(action.getName());
    }

    public Optional<Skill> findSkill(String actionName) {
        return skills.stream().filter(skill -> skill.supports(actionName)).findFirst();
    }

    public List<Skill> getSkills() {
        return List.copyOf(skills);
    }
}
