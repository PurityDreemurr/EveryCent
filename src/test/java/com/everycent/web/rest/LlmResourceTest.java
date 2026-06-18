package com.everycent.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everycent.domain.User;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.AiAlertRequestDTO;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.TransactionParseRequestDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.service.LlmParsingService;
import com.everycent.service.UserService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LlmResourceTest {

    @Mock
    private LlmParsingService llmParsingService;

    @Mock
    private UserService userService;

    private LlmResource resource;
    private User currentUser;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resource = new LlmResource(llmParsingService, userService);
        objectMapper = new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders
            .standaloneSetup(resource)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
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
    void parseTransactionApiShouldAcceptJsonAndReturnStructuredPreview() throws Exception {
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        request.setLedgerId(10L);
        request.setText("今天午饭花了 50 元，很开心");
        request.setTransactionDate(LocalDate.of(2026, 6, 18));

        TransactionParseResultDTO serviceResult = new TransactionParseResultDTO();
        serviceResult.setAmount(new BigDecimal("50.00"));
        serviceResult.setType(TransactionType.EXPENSE);
        serviceResult.setBehaviorTagCode("FOOD");
        serviceResult.setBehaviorTagName("餐饮");
        serviceResult.setEmotionTagCode("HAPPY");
        serviceResult.setEmotionTagName("开心");
        serviceResult.setTransactionDate(LocalDate.of(2026, 6, 18));
        serviceResult.setDescription("午饭");
        serviceResult.setConfidence(0.95);
        serviceResult.setNeedUserConfirm(false);
        serviceResult.setRawInput(request.getText());
        when(llmParsingService.parseTransaction(org.mockito.ArgumentMatchers.any(TransactionParseRequestDTO.class), org.mockito.ArgumentMatchers.same(currentUser)))
            .thenReturn(serviceResult);

        mockMvc
            .perform(post("/api/ai/transaction/parse").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value("50.00"))
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andExpect(jsonPath("$.behaviorTagCode").value("FOOD"))
            .andExpect(jsonPath("$.behaviorTagName").value("餐饮"))
            .andExpect(jsonPath("$.emotionTagCode").value("HAPPY"))
            .andExpect(jsonPath("$.emotionTagName").value("开心"))
            .andExpect(jsonPath("$.transactionDate").value("2026-06-18"))
            .andExpect(jsonPath("$.description").value("午饭"))
            .andExpect(jsonPath("$.confidence").value(0.95))
            .andExpect(jsonPath("$.needUserConfirm").value(false))
            .andExpect(jsonPath("$.rawInput").value("今天午饭花了 50 元，很开心"));
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
