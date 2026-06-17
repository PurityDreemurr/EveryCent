package com.everycent.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.User;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import com.everycent.repository.UserRepository;
import com.everycent.service.LlmParsingService;
import com.everycent.service.TransactionRecordService;
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
        when(llmParsingService.parseAndCreateTransaction(10L, request, currentUser)).thenReturn(serviceResult);

        ResponseEntity<NaturalLanguageTransactionCreateResultDTO> response = resource.createFromNaturalLanguage(10L, request);

        assertThat(response.getBody()).isSameAs(serviceResult);
        verify(llmParsingService).parseAndCreateTransaction(10L, request, currentUser);
    }
}
