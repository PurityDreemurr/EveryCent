package com.everycent.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.User;
import com.everycent.llm.dto.AiAlertRequestDTO;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.TransactionParseRequestDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.service.LlmParsingService;
import com.everycent.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class LlmResourceTest {

    @Mock
    private LlmParsingService llmParsingService;

    @Mock
    private UserService userService;

    private LlmResource resource;
    private User currentUser;

    @BeforeEach
    void setUp() {
        resource = new LlmResource(llmParsingService, userService);
        currentUser = new User();
        currentUser.setLogin("user");
        when(userService.getUserWithAuthorities()).thenReturn(Optional.of(currentUser));
    }

    @Test
    void parseTransactionShouldDelegateToServiceWithCurrentUser() {
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        TransactionParseResultDTO serviceResult = new TransactionParseResultDTO();
        when(llmParsingService.parseTransaction(request, currentUser)).thenReturn(serviceResult);

        ResponseEntity<TransactionParseResultDTO> response = resource.parseTransaction(request);

        assertThat(response.getBody()).isSameAs(serviceResult);
        verify(llmParsingService).parseTransaction(request, currentUser);
    }

    @Test
    void generateBudgetAlertShouldDelegateToServiceWithCurrentUser() {
        AiAlertRequestDTO request = new AiAlertRequestDTO();
        AiAlertResultDTO serviceResult = new AiAlertResultDTO();
        when(llmParsingService.generateBudgetAlert(request, currentUser)).thenReturn(serviceResult);

        ResponseEntity<AiAlertResultDTO> response = resource.generateBudgetAlert(request);

        assertThat(response.getBody()).isSameAs(serviceResult);
        verify(llmParsingService).generateBudgetAlert(request, currentUser);
    }
}
