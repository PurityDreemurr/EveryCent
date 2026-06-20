package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.everycent.domain.User;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import com.everycent.repository.UserRepository;
import com.everycent.service.LlmParsingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NaturalLanguageAccountingSkillTest {

    private User user;

    private SkillExecutionContext context;

    private SkillCurrentUserResolver currentUserResolver;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLogin("alice");

        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        currentUserResolver = new SkillCurrentUserResolver(userRepository);
        context = new SkillExecutionContext();
        context.setUserId(1L);
    }

    @Test
    void shouldCreateTransactionFromConfirmedNaturalLanguageInput() {
        LlmParsingService llmParsingService = org.mockito.Mockito.mock(LlmParsingService.class);
        NaturalLanguageAccountingSkill skill = new NaturalLanguageAccountingSkill(llmParsingService, currentUserResolver);
        NaturalLanguageTransactionCreateResultDTO serviceResult = new NaturalLanguageTransactionCreateResultDTO();
        serviceResult.setTransactionId(99L);
        serviceResult.setAmount(new BigDecimal("28.00"));
        serviceResult.setType(TransactionType.EXPENSE);
        when(
            llmParsingService.parseAndCreateTransaction(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(user)
            )
        )
            .thenReturn(serviceResult);

        SkillResult result = skill.execute(
            new AssistantAction(
                "transaction.create_from_text",
                Map.of("ledgerId", 10L, "text", "昨天午饭 28 元", "transactionDate", "2026-06-19", "confirm", true)
            ),
            context
        );

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(serviceResult);
        verify(llmParsingService)
            .parseAndCreateTransaction(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.argThat(request -> matchesNaturalLanguageRequest(request)),
                org.mockito.ArgumentMatchers.eq(user)
            );
    }

    @Test
    void routerShouldExecuteNaturalLanguageSkillAfterConfirmation() {
        LlmParsingService llmParsingService = org.mockito.Mockito.mock(LlmParsingService.class);
        NaturalLanguageAccountingSkill skill = new NaturalLanguageAccountingSkill(llmParsingService, currentUserResolver);
        SkillRouter router = new SkillRouter(new SkillRegistry(List.of(skill)), new ActionPolicyService());
        when(
            llmParsingService.parseAndCreateTransaction(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(user)
            )
        )
            .thenReturn(new NaturalLanguageTransactionCreateResultDTO());

        SkillResult result = router.route(
            new AssistantAction("transaction.create_from_text", Map.of("ledgerId", 10L, "text", "咖啡 18 元", "confirm", true)),
            context
        );

        assertThat(result.getSuccess()).isTrue();
        verify(llmParsingService)
            .parseAndCreateTransaction(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.argThat(request ->
                    request instanceof NaturalLanguageTransactionCreateRequestDTO
                        && "咖啡 18 元".equals(request.getText())
                        && request.getTransactionDate() == null
                        && Boolean.TRUE.equals(request.getConfirm())
                ),
                org.mockito.ArgumentMatchers.eq(user)
            );
    }

    @Test
    void routerShouldRejectUnconfirmedNaturalLanguageCreateBeforeSkillExecution() {
        LlmParsingService llmParsingService = org.mockito.Mockito.mock(LlmParsingService.class);
        NaturalLanguageAccountingSkill skill = new NaturalLanguageAccountingSkill(llmParsingService, currentUserResolver);
        SkillRouter router = new SkillRouter(new SkillRegistry(List.of(skill)), new ActionPolicyService());

        SkillResult result = router.route(
            new AssistantAction("transaction.create_from_text", Map.of("ledgerId", 10L, "text", "咖啡 18 元", "confirm", false)),
            context
        );

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("INVALID_ACTION");
        assertThat(result.getMessage()).contains("confirm=true");
        verifyNoInteractions(llmParsingService);
    }

    private boolean matchesNaturalLanguageRequest(NaturalLanguageTransactionCreateRequestDTO request) {
        return request != null
            && "昨天午饭 28 元".equals(request.getText())
            && LocalDate.of(2026, 6, 19).equals(request.getTransactionDate())
            && Boolean.TRUE.equals(request.getConfirm());
    }
}
