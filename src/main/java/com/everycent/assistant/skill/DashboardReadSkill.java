package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.service.DashboardService;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DashboardReadSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of(
        "dashboard.summary",
        "dashboard.trend",
        "dashboard.behavior_tags",
        "dashboard.emotion_tags"
    );

    private final DashboardService dashboardService;

    private final SkillCurrentUserResolver currentUserResolver;

    public DashboardReadSkill(DashboardService dashboardService, SkillCurrentUserResolver currentUserResolver) {
        this.dashboardService = dashboardService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "dashboard-read-skill";
    }

    @Override
    public String description() {
        return "Read dashboard summary, trend and tag statistics.";
    }

    @Override
    public boolean supports(String actionName) {
        return ACTIONS.contains(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        User user = currentUserResolver.resolve(context);
        ActionArgumentReader args = new ActionArgumentReader(action);
        Long ledgerId = args.longValue("ledgerId");
        return switch (action.getName()) {
            case "dashboard.summary" -> SkillResult.success(
                action.getName(),
                dashboardService.getSummary(user, ledgerId, args.stringValue("period"), args.dateValue("date"))
            );
            case "dashboard.trend" -> SkillResult.success(
                action.getName(),
                dashboardService.getTrend(user, ledgerId, args.dateValue("startDate"), args.dateValue("endDate"))
            );
            case "dashboard.behavior_tags" -> SkillResult.success(
                action.getName(),
                dashboardService.getBehaviorTagStats(user, ledgerId, args.stringValue("period"), args.dateValue("date"))
            );
            case "dashboard.emotion_tags" -> SkillResult.success(
                action.getName(),
                dashboardService.getEmotionTagStats(user, ledgerId, args.stringValue("period"), args.dateValue("date"))
            );
            default -> SkillResult.failure(action.getName(), "ACTION_NOT_SUPPORTED", "DashboardReadSkill 不支持该 action");
        };
    }
}
