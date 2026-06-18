package com.everycent.service;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.config.LlmProperties;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.EmotionTagRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class AiResultGuardService {

    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final int RAW_INPUT_MAX_LENGTH = 500;
    private static final int PAST_YEAR_LIMIT = 10;
    private static final int FUTURE_YEAR_LIMIT = 1;

    private final BehaviorTagRepository behaviorTagRepository;
    private final EmotionTagRepository emotionTagRepository;
    private final LlmProperties llmProperties;

    public AiResultGuardService(
        BehaviorTagRepository behaviorTagRepository,
        EmotionTagRepository emotionTagRepository,
        LlmProperties llmProperties
    ) {
        this.behaviorTagRepository = behaviorTagRepository;
        this.emotionTagRepository = emotionTagRepository;
        this.llmProperties = llmProperties;
    }

    public TransactionParseResultDTO validateTransactionResult(TransactionParseResultDTO result, Long ledgerId, User currentUser) {
        if (result == null) {
            throw new InvalidAiResultException("AI 解析结果不能为空");
        }
        validateAmount(result.getAmount());
        validateType(result.getType());
        BehaviorTag behaviorTag = validateBehaviorTag(result.getBehaviorTagCode());
        EmotionTag emotionTag = validateEmotionTag(result.getEmotionTagCode());
        validateTransactionDate(result.getTransactionDate());

        result.setBehaviorTagId(behaviorTag.getId());
        result.setBehaviorTagName(behaviorTag.getName());
        result.setEmotionTagId(emotionTag.getId());
        result.setEmotionTagName(emotionTag.getName());
        result.setDescription(sanitizeText(result.getDescription(), DESCRIPTION_MAX_LENGTH));
        result.setRawInput(sanitizeText(result.getRawInput(), rawInputMaxLength()));
        result.setNeedUserConfirm(
            Boolean.TRUE.equals(result.getNeedUserConfirm()) || result.getConfidence() == null || result.getConfidence() < llmProperties.getMinConfidence()
        );
        return result;
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAiResultException("AI 返回金额必须存在且大于 0");
        }
    }

    public void validateType(TransactionType type) {
        if (type == null) {
            throw new InvalidAiResultException("AI 返回交易类型不能为空");
        }
    }

    public BehaviorTag validateBehaviorTag(String behaviorTagCode) {
        return behaviorTagRepository
            .findOneByCode(behaviorTagCode)
            .orElseThrow(() -> new InvalidAiResultException("AI 返回行为标签不存在：" + behaviorTagCode));
    }

    public EmotionTag validateEmotionTag(String emotionTagCode) {
        return emotionTagRepository
            .findOneByCode(emotionTagCode)
            .orElseThrow(() -> new InvalidAiResultException("AI 返回情绪标签不存在：" + emotionTagCode));
    }

    public void validateTransactionDate(LocalDate transactionDate) {
        if (transactionDate == null) {
            throw new InvalidAiResultException("AI 返回交易日期不能为空");
        }
        LocalDate today = LocalDate.now();
        if (transactionDate.isBefore(today.minusYears(PAST_YEAR_LIMIT)) || transactionDate.isAfter(today.plusYears(FUTURE_YEAR_LIMIT))) {
            throw new InvalidAiResultException("AI 返回交易日期超出合理范围");
        }
    }

    public AiAlertResultDTO validateAlertResult(AiAlertResultDTO result) {
        if (result == null) {
            throw new InvalidAiResultException("AI 提醒结果不能为空");
        }
        if (result.getLevel() == null) {
            throw new InvalidAiResultException("AI 提醒级别不能为空");
        }
        result.setTitle(sanitizeRequiredText(result.getTitle(), 100, "AI 提醒标题不能为空"));
        result.setContent(sanitizeRequiredText(result.getContent(), DESCRIPTION_MAX_LENGTH, "AI 提醒正文不能为空"));
        if (result.getNeedNotification() == null) {
            result.setNeedNotification(false);
        }
        return result;
    }

    public String sanitizeText(String text) {
        return sanitizeText(text, DESCRIPTION_MAX_LENGTH);
    }

    private String sanitizeText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String sanitized = text.replace('\r', ' ').replace('\n', ' ').trim();
        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
    }

    private String sanitizeRequiredText(String text, int maxLength, String emptyMessage) {
        String sanitized = sanitizeText(text, maxLength);
        if (sanitized == null || sanitized.isBlank()) {
            throw new InvalidAiResultException(emptyMessage);
        }
        return sanitized;
    }

    private int rawInputMaxLength() {
        return llmProperties.getMaxInputLength() == null ? RAW_INPUT_MAX_LENGTH : llmProperties.getMaxInputLength();
    }
}
