package com.everycent.llm.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.domain.enumeration.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LlmDtoUnitTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void transactionParseRequestShouldValidateRequiredFields() {
        TransactionParseRequestDTO dto = new TransactionParseRequestDTO();
        dto.setText("");

        assertThat(validator.validate(dto))
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("ledgerId", "text");
    }

    @Test
    void transactionParseRequestShouldRejectTextOver500Characters() {
        TransactionParseRequestDTO dto = new TransactionParseRequestDTO();
        dto.setLedgerId(1L);
        dto.setText("a".repeat(501));

        assertThat(validator.validate(dto))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactly("text");
    }

    @Test
    void transactionParseRequestShouldSerializeAndDeserializeJson() throws Exception {
        TransactionParseRequestDTO dto = new TransactionParseRequestDTO();
        dto.setLedgerId(1L);
        dto.setText("午饭 25 元");
        dto.setTransactionDate(LocalDate.of(2026, 6, 16));

        String json = objectMapper.writeValueAsString(dto);
        TransactionParseRequestDTO restored = objectMapper.readValue(json, TransactionParseRequestDTO.class);

        assertThat(json).contains("\"ledgerId\":1", "\"text\":\"午饭 25 元\"", "\"transactionDate\":\"2026-06-16\"");
        assertThat(restored.getLedgerId()).isEqualTo(1L);
        assertThat(restored.getText()).isEqualTo("午饭 25 元");
        assertThat(restored.getTransactionDate()).isEqualTo(LocalDate.of(2026, 6, 16));
    }

    @Test
    void transactionParseResultShouldKeepDocumentFields() {
        TransactionParseResultDTO dto = new TransactionParseResultDTO();
        dto.setAmount(new BigDecimal("25.50"));
        dto.setType(TransactionType.EXPENSE);
        dto.setBehaviorTagCode("FOOD");
        dto.setBehaviorTagName("餐饮");
        dto.setBehaviorTagId(11L);
        dto.setEmotionTagCode("HAPPY");
        dto.setEmotionTagName("开心");
        dto.setEmotionTagId(21L);
        dto.setTransactionDate(LocalDate.of(2026, 6, 16));
        dto.setDescription("午饭");
        dto.setConfidence(0.92);
        dto.setNeedUserConfirm(false);
        dto.setRawInput("午饭 25.5");

        assertThat(dto.getAmount()).isEqualByComparingTo("25.50");
        assertThat(dto.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(dto.getBehaviorTagCode()).isEqualTo("FOOD");
        assertThat(dto.getBehaviorTagName()).isEqualTo("餐饮");
        assertThat(dto.getBehaviorTagId()).isEqualTo(11L);
        assertThat(dto.getEmotionTagCode()).isEqualTo("HAPPY");
        assertThat(dto.getEmotionTagName()).isEqualTo("开心");
        assertThat(dto.getEmotionTagId()).isEqualTo(21L);
        assertThat(dto.getTransactionDate()).isEqualTo(LocalDate.of(2026, 6, 16));
        assertThat(dto.getDescription()).isEqualTo("午饭");
        assertThat(dto.getConfidence()).isEqualTo(0.92);
        assertThat(dto.getNeedUserConfirm()).isFalse();
        assertThat(dto.getRawInput()).isEqualTo("午饭 25.5");
    }

    @Test
    void transactionParseResultShouldSerializeJson() throws Exception {
        TransactionParseResultDTO dto = new TransactionParseResultDTO();
        dto.setAmount(new BigDecimal("25.50"));
        dto.setType(TransactionType.EXPENSE);
        dto.setTransactionDate(LocalDate.of(2026, 6, 16));
        dto.setNeedUserConfirm(false);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"amount\":\"25.50\"", "\"type\":\"EXPENSE\"", "\"transactionDate\":\"2026-06-16\"", "\"needUserConfirm\":false");
    }

    @Test
    void aiAlertRequestShouldValidateRequiredFields() {
        AiAlertRequestDTO dto = new AiAlertRequestDTO();

        assertThat(validator.validate(dto))
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("ledgerId", "budgetId");
    }

    @Test
    void aiAlertRequestShouldKeepFieldsAndSerializeJson() throws Exception {
        AiAlertRequestDTO dto = new AiAlertRequestDTO();
        dto.setLedgerId(1L);
        dto.setBudgetId(2L);
        dto.setSaveAsNotification(true);

        String json = objectMapper.writeValueAsString(dto);
        AiAlertRequestDTO restored = objectMapper.readValue(json, AiAlertRequestDTO.class);

        assertThat(json).contains("\"ledgerId\":1", "\"budgetId\":2", "\"saveAsNotification\":true");
        assertThat(restored.getLedgerId()).isEqualTo(1L);
        assertThat(restored.getBudgetId()).isEqualTo(2L);
        assertThat(restored.getSaveAsNotification()).isTrue();
    }

    @Test
    void aiAlertResultShouldKeepDocumentFieldsAndSerializeJson() throws Exception {
        AiAlertResultDTO dto = new AiAlertResultDTO();
        dto.setTitle("预算预警");
        dto.setContent("本月餐饮预算已使用 85%");
        dto.setLevel(AiAlertResultDTO.AlertLevel.WARNING);
        dto.setOverBudget(false);
        dto.setUsedAmount(new BigDecimal("850.00"));
        dto.setLimitAmount(new BigDecimal("1000.00"));
        dto.setUsedRatio(new BigDecimal("0.85"));
        dto.setNeedNotification(true);
        dto.setNotificationId(99L);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(dto.getTitle()).isEqualTo("预算预警");
        assertThat(dto.getContent()).isEqualTo("本月餐饮预算已使用 85%");
        assertThat(dto.getLevel()).isEqualTo(AiAlertResultDTO.AlertLevel.WARNING);
        assertThat(dto.getOverBudget()).isFalse();
        assertThat(dto.getUsedAmount()).isEqualByComparingTo("850.00");
        assertThat(dto.getLimitAmount()).isEqualByComparingTo("1000.00");
        assertThat(dto.getUsedRatio()).isEqualByComparingTo("0.85");
        assertThat(dto.getNeedNotification()).isTrue();
        assertThat(dto.getNotificationId()).isEqualTo(99L);
        assertThat(json).contains(
            "\"title\":\"预算预警\"",
            "\"content\":\"本月餐饮预算已使用 85%\"",
            "\"level\":\"WARNING\"",
            "\"overBudget\":false",
            "\"usedAmount\":850.00",
            "\"limitAmount\":1000.00",
            "\"usedRatio\":0.85",
            "\"needNotification\":true",
            "\"notificationId\":99"
        );
    }
}
