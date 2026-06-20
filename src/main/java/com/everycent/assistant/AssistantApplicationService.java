package com.everycent.assistant;

import com.everycent.assistant.ResponseRenderer.RenderedAssistantResponse;
import com.everycent.assistant.dto.AssistantSkillResultDTO;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.assistant.dto.MemoryContextDTO;
import com.everycent.assistant.skill.AssistantAction;
import com.everycent.assistant.skill.AssistantPlan;
import com.everycent.assistant.skill.SkillExecutionContext;
import com.everycent.assistant.skill.SkillResult;
import com.everycent.assistant.skill.SkillRouter;
import com.everycent.domain.User;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssistantApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantApplicationService.class);

    private final AssistantPlanner assistantPlanner;

    private final SkillRouter skillRouter;

    private final ResponseRenderer responseRenderer;

    private final AiAssistantOrchestrator aiAssistantOrchestrator;

    public AssistantApplicationService(
        AssistantPlanner assistantPlanner,
        SkillRouter skillRouter,
        ResponseRenderer responseRenderer,
        AiAssistantOrchestrator aiAssistantOrchestrator
    ) {
        this.assistantPlanner = assistantPlanner;
        this.skillRouter = skillRouter;
        this.responseRenderer = responseRenderer;
        this.aiAssistantOrchestrator = aiAssistantOrchestrator;
    }

    public ChatResponseDTO chat(User currentUser, ChatRequestDTO request) {
        AssistantPlan plan = assistantPlanner.plan(currentUser, request);
        LOG.info("Assistant plan generated intent={}, actions={}", plan.getIntent(), actionNames(plan));
        if (plan.getActions().isEmpty()) {
            LOG.info("Assistant plan has no skill actions; fallback to LLM chat. intent={}", plan.getIntent());
            ChatResponseDTO response = aiAssistantOrchestrator.chat(currentUser, request);
            response.setResponseType("message");
            return response;
        }

        SkillExecutionContext context = executionContext(currentUser, request);
        List<SkillResult> results = plan
            .getActions()
            .stream()
            .sorted(Comparator.comparing(action -> action.getPriority() == null ? 0 : action.getPriority()))
            .map(action -> execute(action, context))
            .toList();

        LOG.info("Assistant skill results={}", resultSummary(results));
        RenderedAssistantResponse rendered = responseRenderer.render(plan, results);
        return response(currentUser, request, rendered, results);
    }

    private SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        try {
            return skillRouter.route(action, context);
        } catch (RuntimeException e) {
            LOG.warn("Assistant skill execution failed action={}, error={}", action.getName(), e.getClass().getSimpleName());
            return SkillResult.failure(action.getName(), "SKILL_EXECUTION_FAILED", "操作执行失败，请稍后重试或检查信息是否完整。");
        }
    }

    private SkillExecutionContext executionContext(User currentUser, ChatRequestDTO request) {
        SkillExecutionContext context = new SkillExecutionContext();
        context.setUserId(currentUser == null ? null : currentUser.getId());
        context.setDefaultLedgerId(request == null ? null : request.getLedgerId());
        context.setSessionId(request == null || request.getConversationId() == null ? null : String.valueOf(request.getConversationId()));
        context.setOriginalInput(request == null ? null : request.getMessage());
        return context;
    }

    private ChatResponseDTO response(User currentUser, ChatRequestDTO request, RenderedAssistantResponse rendered, List<SkillResult> results) {
        ChatResponseDTO response = new ChatResponseDTO();
        response.setConversationId(request.getConversationId() == null ? System.currentTimeMillis() : request.getConversationId());
        response.setMessageId(System.nanoTime());
        response.setAssistantMessage(rendered.assistantMessage());
        response.setResponseType(rendered.responseType());
        response.setCards(rendered.cards());
        response.setSkillResults(results.stream().map(AssistantSkillResultDTO::from).toList());
        response.setAccountingCapture(rendered.accountingCapture());
        response.setUserEmotionTagCode("NEUTRAL");
        response.setUserEmotionConfidence(1.0);
        response.setAiEmotionBefore("neutral");
        response.setAiEmotionAfter("neutral");
        response.setRetrievedMemories(List.of());
        return response;
    }

    private List<String> actionNames(AssistantPlan plan) {
        return plan.getActions().stream().map(AssistantAction::getName).toList();
    }

    private List<String> resultSummary(List<SkillResult> results) {
        return results.stream().map(result -> result.getActionName() + ":" + result.getSuccess() + ":" + result.getErrorCode()).toList();
    }
}
