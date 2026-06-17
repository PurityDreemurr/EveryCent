package com.everycent.llm.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LlmJsonResponseParserTest {

    private final LlmJsonResponseParser parser = new LlmJsonResponseParser(new ObjectMapper());

    @Test
    void shouldParseTransactionJsonFromAiResponse() {
        String aiResponse = """
            ```json
            {
              "amount": "50.00",
              "type": "EXPENSE",
              "behaviorTagCode": "FOOD",
              "emotionTagCode": "HAPPY",
              "transactionDate": "2026-06-15",
              "description": "中午吃饭",
              "confidence": 0.92
            }
            ```
            """;

        TransactionParseResultDTO result = parser.parseTransaction(aiResponse);

        assertThat(result.getAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.getBehaviorTagCode()).isEqualTo("FOOD");
        assertThat(result.getEmotionTagCode()).isEqualTo("HAPPY");
        assertThat(result.getTransactionDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(result.getDescription()).isEqualTo("中午吃饭");
        assertThat(result.getConfidence()).isEqualTo(0.92);
    }

    @Test
    void shouldRejectAiResponseMissingRequiredField() {
        String aiResponse = """
            {
              "amount": "50.00",
              "type": "EXPENSE",
              "behaviorTagCode": "FOOD",
              "emotionTagCode": "HAPPY",
              "transactionDate": "2026-06-15",
              "confidence": 0.92
            }
            """;

        assertThatThrownBy(() -> parser.parseTransaction(aiResponse))
            .isInstanceOf(LlmParseException.class)
            .hasMessageContaining("description");
    }
}
