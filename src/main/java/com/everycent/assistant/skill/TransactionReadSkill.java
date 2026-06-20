package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.llm.dto.TransactionParseRequestDTO;
import com.everycent.service.LlmParsingService;
import com.everycent.service.TransactionRecordService;
import com.everycent.service.dto.TransactionQueryDTO;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TransactionReadSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("transaction.list", "transaction.get", "transaction.parse");

    private final TransactionRecordService transactionRecordService;

    private final LlmParsingService llmParsingService;

    private final SkillCurrentUserResolver currentUserResolver;

    public TransactionReadSkill(
        TransactionRecordService transactionRecordService,
        LlmParsingService llmParsingService,
        SkillCurrentUserResolver currentUserResolver
    ) {
        this.transactionRecordService = transactionRecordService;
        this.llmParsingService = llmParsingService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "transaction-read-skill";
    }

    @Override
    public String description() {
        return "Read transactions and parse transaction text without creating records.";
    }

    @Override
    public boolean supports(String actionName) {
        return ACTIONS.contains(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        User user = currentUserResolver.resolve(context);
        ActionArgumentReader args = new ActionArgumentReader(action);
        return switch (action.getName()) {
            case "transaction.list" -> SkillResult.success(
                action.getName(),
                transactionRecordService.findByLedger(user, args.longValue("ledgerId"), transactionQuery(args))
            );
            case "transaction.get" -> SkillResult.success(action.getName(), transactionRecordService.findOne(user, args.longValue("transactionId")));
            case "transaction.parse" -> SkillResult.success(action.getName(), llmParsingService.parseTransaction(parseRequest(args), user));
            default -> SkillResult.failure(action.getName(), "ACTION_NOT_SUPPORTED", "TransactionReadSkill 不支持该 action");
        };
    }

    private TransactionQueryDTO transactionQuery(ActionArgumentReader args) {
        TransactionQueryDTO query = new TransactionQueryDTO();
        query.setStartDate(args.dateValue("startDate"));
        query.setEndDate(args.dateValue("endDate"));
        query.setType(args.transactionType("type"));
        query.setBehaviorTagId(args.longValue("behaviorTagId"));
        query.setEmotionTagId(args.longValue("emotionTagId"));
        query.setPage(args.intValue("page", 0));
        query.setSize(args.intValue("size", 20));
        return query;
    }

    private TransactionParseRequestDTO parseRequest(ActionArgumentReader args) {
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        request.setLedgerId(args.longValue("ledgerId"));
        request.setText(args.stringValue("text"));
        request.setTransactionDate(args.dateValue("transactionDate"));
        return request;
    }
}
