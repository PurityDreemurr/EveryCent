package com.everycent.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.User;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import com.everycent.repository.UserRepository;
import com.everycent.service.LlmParsingService;
import com.everycent.service.TransactionRecordService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TransactionRecordResourceTest {

    @Mock
    private TransactionRecordService transactionRecordService;

    @Mock
    private LlmParsingService llmParsingService;

    @Mock
    private UserRepository userRepository;

    private TransactionRecordResource resource;
    private User currentUser;

    @BeforeEach
    void setUp() {
        resource = new TransactionRecordResource(transactionRecordService, userRepository, llmParsingService);
        currentUser = new User();
        currentUser.setLogin("user");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));
        when(userRepository.findOneByLogin("user")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createFromNaturalLanguageShouldDelegateToServiceWithCurrentUser() {
        NaturalLanguageTransactionCreateRequestDTO request = new NaturalLanguageTransactionCreateRequestDTO();
        NaturalLanguageTransactionCreateResultDTO serviceResult = new NaturalLanguageTransactionCreateResultDTO();
        serviceResult.setTransactionId(101L);
        serviceResult.setAmount(new BigDecimal("50.00"));
        serviceResult.setType(TransactionType.EXPENSE);
        serviceResult.setBehaviorTagName("餐饮");
        serviceResult.setEmotionTagName("开心");
        NaturalLanguageTransactionCreateResultDTO.ParsedResultDTO parsedResult = new NaturalLanguageTransactionCreateResultDTO.ParsedResultDTO();
        parsedResult.setAmount(new BigDecimal("50.00"));
        parsedResult.setType(TransactionType.EXPENSE);
        parsedResult.setBehaviorTag("餐饮");
        parsedResult.setEmotionTag("开心");
        serviceResult.setParsedResult(parsedResult);
        NaturalLanguageTransactionCreateResultDTO.BudgetWarningDTO budgetWarning = new NaturalLanguageTransactionCreateResultDTO.BudgetWarningDTO();
        budgetWarning.setOverBudget(false);
        budgetWarning.setUsedRatio(new BigDecimal("0.0500"));
        budgetWarning.setMessage("本期预算使用正常");
        serviceResult.setBudgetWarning(budgetWarning);
        when(llmParsingService.parseAndCreateTransaction(10L, request, currentUser)).thenReturn(serviceResult);

        ResponseEntity<NaturalLanguageTransactionCreateResultDTO> response = resource.createFromNaturalLanguage(10L, request);

        assertThat(response.getBody()).isSameAs(serviceResult);
        assertThat(response.getBody().getParsedResult()).isNotNull();
        assertThat(response.getBody().getParsedResult().getAmount()).isEqualByComparingTo("50.00");
        assertThat(response.getBody().getParsedResult().getBehaviorTag()).isEqualTo("餐饮");
        assertThat(response.getBody().getParsedResult().getEmotionTag()).isEqualTo("开心");
        assertThat(response.getBody().getBudgetWarning()).isNotNull();
        assertThat(response.getBody().getBudgetWarning().getOverBudget()).isFalse();
        assertThat(response.getBody().getBudgetWarning().getUsedRatio()).isEqualByComparingTo("0.0500");
        verify(llmParsingService).parseAndCreateTransaction(10L, request, currentUser);
    }
}
