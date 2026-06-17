package com.everycent.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.User;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
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
class TransactionRecordResourceTest {

    @Mock
    private LlmParsingService llmParsingService;

    @Mock
    private UserService userService;

    private TransactionRecordResource resource;
    private User currentUser;

    @BeforeEach
    void setUp() {
        resource = new TransactionRecordResource(llmParsingService, userService);
        currentUser = new User();
        currentUser.setLogin("user");
        when(userService.getUserWithAuthorities()).thenReturn(Optional.of(currentUser));
    }

    @Test
    void createFromNaturalLanguageShouldDelegateToServiceWithCurrentUser() {
        NaturalLanguageTransactionCreateRequestDTO request = new NaturalLanguageTransactionCreateRequestDTO();
        NaturalLanguageTransactionCreateResultDTO serviceResult = new NaturalLanguageTransactionCreateResultDTO();
        when(llmParsingService.parseAndCreateTransaction(10L, request, currentUser)).thenReturn(serviceResult);

        ResponseEntity<NaturalLanguageTransactionCreateResultDTO> response = resource.createFromNaturalLanguage(10L, request);

        assertThat(response.getBody()).isSameAs(serviceResult);
        verify(llmParsingService).parseAndCreateTransaction(10L, request, currentUser);
    }
}
