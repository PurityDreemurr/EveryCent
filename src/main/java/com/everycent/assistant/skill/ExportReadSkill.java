package com.everycent.assistant.skill;

import com.everycent.config.QqBotProperties;
import com.everycent.domain.User;
import com.everycent.service.ExportDownloadTokenService;
import com.everycent.service.ExcelExportService;
import java.util.Map;
import org.springframework.stereotype.Component;
import tech.jhipster.config.JHipsterProperties;

@Component
public class ExportReadSkill implements Skill {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelExportService excelExportService;

    private final SkillCurrentUserResolver currentUserResolver;

    private final ExportDownloadTokenService exportDownloadTokenService;

    private final QqBotProperties qqBotProperties;

    private final JHipsterProperties jHipsterProperties;

    public ExportReadSkill(
        ExcelExportService excelExportService,
        SkillCurrentUserResolver currentUserResolver,
        ExportDownloadTokenService exportDownloadTokenService,
        QqBotProperties qqBotProperties,
        JHipsterProperties jHipsterProperties
    ) {
        this.excelExportService = excelExportService;
        this.currentUserResolver = currentUserResolver;
        this.exportDownloadTokenService = exportDownloadTokenService;
        this.qqBotProperties = qqBotProperties;
        this.jHipsterProperties = jHipsterProperties;
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
        Long ledgerId = args.longValue("ledgerId");
        java.time.LocalDate startDate = args.dateValue("startDate");
        java.time.LocalDate endDate = args.dateValue("endDate");
        byte[] bytes = excelExportService.exportTransactions(user, ledgerId, startDate, endDate);
        String token = exportDownloadTokenService.create(user.getId(), ledgerId, startDate, endDate);
        String downloadPath = "/api/public/exports/transactions/" + token;
        return SkillResult.success(
            action.getName(),
            Map.of(
                "downloadReady",
                true,
                "contentType",
                CONTENT_TYPE,
                "byteLength",
                bytes.length,
                "fileName",
                "everycent-transactions.xlsx",
                "ledgerId",
                ledgerId,
                "startDate",
                startDate.toString(),
                "endDate",
                endDate.toString(),
                "downloadUrl",
                absoluteUrl(downloadPath),
                "expiresInMinutes",
                30
            )
        );
    }

    private String absoluteUrl(String path) {
        String baseUrl = firstText(
            qqBotProperties == null ? null : qqBotProperties.getPublicBaseUrl(),
            jHipsterProperties == null || jHipsterProperties.getMail() == null ? null : jHipsterProperties.getMail().getBaseUrl()
        );
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        return baseUrl.replaceAll("/+$", "") + path;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
