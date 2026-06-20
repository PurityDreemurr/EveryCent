package com.everycent.assistant.skill;

import com.everycent.service.TagService;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TagReadSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("tag.behavior.list", "tag.emotion.list");

    private final TagService tagService;

    public TagReadSkill(TagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public String name() {
        return "tag-read-skill";
    }

    @Override
    public String description() {
        return "Read behavior and emotion tag white lists.";
    }

    @Override
    public boolean supports(String actionName) {
        return ACTIONS.contains(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        return switch (action.getName()) {
            case "tag.behavior.list" -> SkillResult.success(action.getName(), tagService.findBehaviorTags());
            case "tag.emotion.list" -> SkillResult.success(action.getName(), tagService.findEmotionTags());
            default -> SkillResult.failure(action.getName(), "ACTION_NOT_SUPPORTED", "TagReadSkill 不支持该 action");
        };
    }
}
