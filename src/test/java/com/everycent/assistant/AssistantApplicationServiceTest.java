package com.everycent.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.everycent.assistant.dto.ChatHistoryMessageDTO;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.skill.AssistantAction;
import com.everycent.assistant.skill.AssistantIntent;
import com.everycent.assistant.skill.AssistantPlan;
import com.everycent.assistant.skill.SkillExecutionContext;
import com.everycent.assistant.skill.SkillResult;
import com.everycent.assistant.skill.SkillRouter;
import com.everycent.domain.User;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AssistantApplicationServiceTest {

    @Test
    void shouldExecutePlannerActionsThroughSkillRouterAndRenderTransactionCard() {
        AssistantPlanner planner = org.mockito.Mockito.mock(AssistantPlanner.class);
        SkillRouter router = org.mockito.Mockito.mock(SkillRouter.class);
        AssistantApplicationService service = new AssistantApplicationService(
            planner,
            router,
            new ResponseRenderer(),
            org.mockito.Mockito.mock(AiAssistantOrchestrator.class),
            org.mockito.Mockito.mock(AssistantConversationStore.class)
        );
        ChatRequestDTO request = request("午饭28", 10L);
        AssistantPlan plan = plan("transaction.create_from_text");
        NaturalLanguageTransactionCreateResultDTO data = new NaturalLanguageTransactionCreateResultDTO();
        data.setTransactionId(99L);
        data.setAmount(new BigDecimal("28.00"));
        when(planner.plan(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request))).thenReturn(plan);
        when(router.route(org.mockito.ArgumentMatchers.any(AssistantAction.class), org.mockito.ArgumentMatchers.any(SkillExecutionContext.class)))
            .thenReturn(SkillResult.success("transaction.create_from_text", data));

        var response = service.chat(user(), request);

        assertThat(response.getAssistantMessage()).isEqualTo("已记账。");
        assertThat(response.getResponseType()).isEqualTo("transaction_created");
        assertThat(response.getCards()).hasSize(1);
        assertThat(response.getCards().get(0).getType()).isEqualTo("transaction_created");
        assertThat(response.getAccountingCapture().getCreated()).isTrue();
        assertThat(response.getAccountingCapture().getTransactionId()).isEqualTo(99L);
        assertThat(response.getSkillResults()).hasSize(1);
        assertThat(response.getSkillResults().get(0).getActionName()).isEqualTo("transaction.create_from_text");
    }

    @Test
    void shouldRenderMultipleCreatedTransactions() {
        AssistantPlanner planner = org.mockito.Mockito.mock(AssistantPlanner.class);
        SkillRouter router = org.mockito.Mockito.mock(SkillRouter.class);
        AssistantApplicationService service = new AssistantApplicationService(
            planner,
            router,
            new ResponseRenderer(),
            org.mockito.Mockito.mock(AiAssistantOrchestrator.class),
            org.mockito.Mockito.mock(AssistantConversationStore.class)
        );
        ChatRequestDTO request = request("午饭28，咖啡18", 10L);
        NaturalLanguageTransactionCreateResultDTO lunch = new NaturalLanguageTransactionCreateResultDTO();
        lunch.setTransactionId(99L);
        NaturalLanguageTransactionCreateResultDTO coffee = new NaturalLanguageTransactionCreateResultDTO();
        coffee.setTransactionId(100L);
        when(planner.plan(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request))).thenReturn(plan("transaction.create_from_text"));
        when(router.route(org.mockito.ArgumentMatchers.any(AssistantAction.class), org.mockito.ArgumentMatchers.any(SkillExecutionContext.class)))
            .thenReturn(SkillResult.success("transaction.create_from_text", java.util.List.of(lunch, coffee)));

        var response = service.chat(user(), request);

        assertThat(response.getAssistantMessage()).isEqualTo("已记账 2 笔。");
        assertThat(response.getCards().get(0).getType()).isEqualTo("transaction_created");
        assertThat(response.getAccountingCapture().getCreated()).isTrue();
        assertThat(response.getAccountingCapture().getTransactionId()).isEqualTo(99L);
    }

    @Test
    void shouldRenderForbiddenDeleteAsPolicyBlockedCard() {
        AssistantPlanner planner = org.mockito.Mockito.mock(AssistantPlanner.class);
        SkillRouter router = org.mockito.Mockito.mock(SkillRouter.class);
        AssistantApplicationService service = new AssistantApplicationService(
            planner,
            router,
            new ResponseRenderer(),
            org.mockito.Mockito.mock(AiAssistantOrchestrator.class),
            org.mockito.Mockito.mock(AssistantConversationStore.class)
        );
        ChatRequestDTO request = request("删掉午饭", 10L);
        when(planner.plan(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request))).thenReturn(plan("transaction.delete"));
        when(router.route(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(SkillResult.blocked("transaction.delete", "Action forbidden"));

        var response = service.chat(user(), request);

        assertThat(response.getResponseType()).isEqualTo("policy_blocked");
        assertThat(response.getAssistantMessage()).contains("不能由 AI 助手执行");
        assertThat(response.getCards().get(0).getMessage()).contains("手动删除");
    }

    @Test
    void shouldFallbackToLlmChatWhenPlanHasNoSkillActions() {
        AssistantPlanner planner = org.mockito.Mockito.mock(AssistantPlanner.class);
        SkillRouter router = org.mockito.Mockito.mock(SkillRouter.class);
        AiAssistantOrchestrator orchestrator = org.mockito.Mockito.mock(AiAssistantOrchestrator.class);
        AssistantConversationStore conversationStore = org.mockito.Mockito.mock(AssistantConversationStore.class);
        AssistantApplicationService service = new AssistantApplicationService(
            planner,
            router,
            new ResponseRenderer(),
            orchestrator,
            conversationStore
        );
        ChatRequestDTO request = request("我今天有点无聊", 10L);
        AssistantPlan dailyChatPlan = new AssistantPlan();
        dailyChatPlan.setIntent(AssistantIntent.DAILY_CHAT);
        com.everycent.assistant.dto.ChatResponseDTO llmResponse = new com.everycent.assistant.dto.ChatResponseDTO();
        llmResponse.setAssistantMessage("那我陪你待一会儿。{\"mood\":40,\"emoji\":\"peace\"}");
        ChatHistoryMessageDTO previousMessage = new ChatHistoryMessageDTO();
        previousMessage.setRole("user");
        previousMessage.setContent("我今天心情不太好");
        when(conversationStore.ensureConversation(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request))).thenReturn(88L);
        when(conversationStore.recentMessagesForPrompt(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request)))
            .thenReturn(java.util.List.of(previousMessage));
        when(planner.plan(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request))).thenReturn(dailyChatPlan);
        when(
                orchestrator.chat(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.eq(request),
                    org.mockito.ArgumentMatchers.eq(java.util.List.of(previousMessage))
                )
            )
            .thenReturn(llmResponse);

        var response = service.chat(user(), request);

        assertThat(request.getConversationId()).isEqualTo(88L);
        assertThat(response.getAssistantMessage()).contains("陪你");
        assertThat(response.getResponseType()).isEqualTo("message");
        verify(conversationStore).recentMessagesForPrompt(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request));
        verifyNoInteractions(router);
    }

    private AssistantPlan plan(String actionName) {
        AssistantPlan plan = new AssistantPlan();
        plan.setIntent(AssistantIntent.TRANSACTION_RECORD);
        plan.getActions().add(new AssistantAction(actionName));
        return plan;
    }

    private ChatRequestDTO request(String message, Long ledgerId) {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setMessage(message);
        request.setLedgerId(ledgerId);
        return request;
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setLogin("alice");
        return user;
    }
}
