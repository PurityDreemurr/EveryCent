package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.service.LlmParsingService;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NaturalLanguageAccountingSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("transaction.create_from_text");

    private final LlmParsingService llmParsingService;

    private final SkillCurrentUserResolver currentUserResolver;

    private final NaturalLanguageTransactionSplitter transactionSplitter;

    public NaturalLanguageAccountingSkill(
        LlmParsingService llmParsingService,
        SkillCurrentUserResolver currentUserResolver,
        NaturalLanguageTransactionSplitter transactionSplitter
    ) {
        this.llmParsingService = llmParsingService;
        this.currentUserResolver = currentUserResolver;
        this.transactionSplitter = transactionSplitter;
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
        List<String> transactionTexts = transactionSplitter.split(args.stringValue("text"));
        if (transactionTexts.size() <= 1) {
            return SkillResult.success(
                action.getName(),
                llmParsingService.parseAndCreateTransaction(args.longValue("ledgerId"), naturalLanguageRequest(args, args.stringValue("text")), user)
            );
        }
        return SkillResult.success(
            action.getName(),
            transactionTexts
                .stream()
                .map(text -> llmParsingService.parseAndCreateTransaction(args.longValue("ledgerId"), naturalLanguageRequest(args, text), user))
                .toList()
        );
    }

    private NaturalLanguageTransactionCreateRequestDTO naturalLanguageRequest(ActionArgumentReader args, String text) {
        NaturalLanguageTransactionCreateRequestDTO request = new NaturalLanguageTransactionCreateRequestDTO();
        request.setText(text);
        request.setTransactionDate(args.dateValue("transactionDate"));
        request.setConfirm(args.booleanValue("confirm"));
        return request;
    }
}
