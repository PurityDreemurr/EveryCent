package com.everycent.llm.parser;

import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
            requireAny(root, "behaviorTagCode", "behaviorTag");
            requireAny(root, "emotionTagCode", "moodTag", "emotionTag");
            require(root, "transactionDate");
            requireAny(root, "description", "remark");
            require(root, "confidence");

            TransactionParseResultDTO dto = new TransactionParseResultDTO();
            dto.setAmount(new BigDecimal(root.path("amount").asText()));
            dto.setType(TransactionType.valueOf(root.path("type").asText()));
            dto.setBehaviorTagCode(text(root, "behaviorTagCode", "behaviorTag"));
            dto.setEmotionTagCode(text(root, "emotionTagCode", "moodTag", "emotionTag"));
            dto.setTransactionDate(LocalDate.parse(root.path("transactionDate").asText()));
            dto.setDescription(text(root, "description", "remark"));
            dto.setConfidence(root.path("confidence").asDouble());
            if (hasValue(root, "needUserConfirm")) {
                dto.setNeedUserConfirm(root.path("needUserConfirm").asBoolean());
            } else if (hasValue(root, "needsManualReview")) {
                dto.setNeedUserConfirm(root.path("needsManualReview").asBoolean());
            }
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
            if (hasValue(root, "analysisSummary")) {
                dto.setAnalysisSummary(root.path("analysisSummary").asText());
            }
            dto.setMajorExpenses(textArray(root, "majorExpenses"));
            dto.setUnnecessaryExpenses(textArray(root, "unnecessaryExpenses"));
            dto.setSuggestions(textArray(root, "suggestions"));
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

    private void requireAny(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (hasValue(root, fieldName)) {
                return;
            }
        }
        throw new LlmParseException("LLM 返回缺少必填字段：" + String.join("/", fieldNames));
    }

    private boolean hasValue(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        return !node.isMissingNode() && !node.isNull() && (!node.isTextual() || StringUtils.hasText(node.asText()));
    }

    private String text(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (hasValue(root, fieldName)) {
                return root.path(fieldName).asText();
            }
        }
        return null;
    }

    private List<String> textArray(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (!item.isNull() && StringUtils.hasText(item.asText())) {
                values.add(item.asText());
            }
        });
        return values;
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
