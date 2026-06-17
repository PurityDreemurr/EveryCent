package com.everycent.llm.parser;

import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LlmJsonResponseParser {

    private final ObjectMapper objectMapper;

    public LlmJsonResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TransactionParseResultDTO parseTransaction(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(jsonText));
            require(root, "amount");
            require(root, "type");
            require(root, "behaviorTagCode");
            require(root, "emotionTagCode");
            require(root, "transactionDate");
            require(root, "description");
            require(root, "confidence");

            TransactionParseResultDTO dto = new TransactionParseResultDTO();
            dto.setAmount(new BigDecimal(root.path("amount").asText()));
            dto.setType(TransactionType.valueOf(root.path("type").asText()));
            dto.setBehaviorTagCode(root.path("behaviorTagCode").asText());
            dto.setEmotionTagCode(root.path("emotionTagCode").asText());
            dto.setTransactionDate(LocalDate.parse(root.path("transactionDate").asText()));
            dto.setDescription(root.path("description").asText());
            dto.setConfidence(root.path("confidence").asDouble());
            return dto;
        } catch (LlmParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmParseException("LLM 交易解析 JSON 无法解析", e);
        }
    }

    public AiAlertResultDTO parseAlert(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(jsonText));
            require(root, "title");
            require(root, "content");
            require(root, "level");
            require(root, "needNotification");

            AiAlertResultDTO dto = new AiAlertResultDTO();
            dto.setTitle(root.path("title").asText());
            dto.setContent(root.path("content").asText());
            dto.setLevel(AiAlertResultDTO.AlertLevel.valueOf(root.path("level").asText()));
            dto.setNeedNotification(root.path("needNotification").asBoolean());
            return dto;
        } catch (LlmParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmParseException("LLM 提醒 JSON 无法解析", e);
        }
    }

    private void require(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull() || (node.isTextual() && !StringUtils.hasText(node.asText()))) {
            throw new LlmParseException("LLM 返回缺少必填字段：" + fieldName);
        }
    }

    private String extractJson(String jsonText) {
        if (!StringUtils.hasText(jsonText)) {
            throw new LlmParseException("LLM 返回内容为空");
        }
        String text = jsonText.trim();
        if (text.startsWith("```")) {
            int firstLineBreak = text.indexOf('\n');
            int closingFence = text.lastIndexOf("```");
            if (firstLineBreak >= 0 && closingFence > firstLineBreak) {
                text = text.substring(firstLineBreak + 1, closingFence).trim();
            }
        }
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart < 0 || objectEnd <= objectStart) {
            throw new LlmParseException("LLM 返回内容不包含 JSON 对象");
        }
        return text.substring(objectStart, objectEnd + 1);
    }
}
