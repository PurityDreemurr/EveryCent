package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.service.LlmParsingService;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NaturalLanguageAccountingSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("transaction.create_from_text");

    private final LlmParsingService llmParsingService;

    private final SkillCurrentUserResolver currentUserResolver;

    public NaturalLanguageAccountingSkill(LlmParsingService llmParsingService, SkillCurrentUserResolver currentUserResolver) {
        this.llmParsingService = llmParsingService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "natural-language-accounting-skill";
    }

    @Override
    public String description() {
        return "Create a transaction from confirmed natural language accounting input.";
    }

    @Override
    public boolean supports(String actionName) {
        return ACTIONS.contains(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        User user = currentUserResolver.resolve(context);
        ActionArgumentReader args = new ActionArgumentReader(action);
        return SkillResult.success(
            action.getName(),
            llmParsingService.parseAndCreateTransaction(args.longValue("ledgerId"), naturalLanguageRequest(args), user)
        );
    }

    private NaturalLanguageTransactionCreateRequestDTO naturalLanguageRequest(ActionArgumentReader args) {
        NaturalLanguageTransactionCreateRequestDTO request = new NaturalLanguageTransactionCreateRequestDTO();
        request.setText(args.stringValue("text"));
        request.setTransactionDate(args.dateValue("transactionDate"));
        request.setConfirm(args.booleanValue("confirm"));
        return request;
    }
}
