package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.service.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class NotificationReadSkill implements Skill {

    private final NotificationService notificationService;

    private final SkillCurrentUserResolver currentUserResolver;

    public NotificationReadSkill(NotificationService notificationService, SkillCurrentUserResolver currentUserResolver) {
        this.notificationService = notificationService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "notification-read-skill";
    }

    @Override
    public String description() {
        return "Read current user's notifications.";
    }

    @Override
    public boolean supports(String actionName) {
        return "notification.list".equals(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        User user = currentUserResolver.resolve(context);
        ActionArgumentReader args = new ActionArgumentReader(action);
        return SkillResult.success(
            action.getName(),
            notificationService.findForUser(user, args.booleanValue("read"), args.intValue("page", 0), args.intValue("size", 20))
        );
    }
}
