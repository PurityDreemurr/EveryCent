package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.service.ExcelExportService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExportReadSkill implements Skill {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelExportService excelExportService;

    private final SkillCurrentUserResolver currentUserResolver;

    public ExportReadSkill(ExcelExportService excelExportService, SkillCurrentUserResolver currentUserResolver) {
        this.excelExportService = excelExportService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "export-read-skill";
    }

    @Override
    public String description() {
        return "Prepare transaction export without returning binary content to LLM.";
    }

    @Override
    public boolean supports(String actionName) {
        return "export.transactions".equals(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        User user = currentUserResolver.resolve(context);
        ActionArgumentReader args = new ActionArgumentReader(action);
        byte[] bytes = excelExportService.exportTransactions(
            user,
            args.longValue("ledgerId"),
            args.dateValue("startDate"),
            args.dateValue("endDate")
        );
        return SkillResult.success(
            action.getName(),
            Map.of("downloadReady", true, "contentType", CONTENT_TYPE, "byteLength", bytes.length)
        );
    }
}
