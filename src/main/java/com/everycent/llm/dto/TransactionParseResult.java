package com.everycent.llm.dto;

import java.math.BigDecimal;

/**
 * Candidate transaction fields parsed from natural language.
 * TODO: Replace String tags with enums after business entities are created.
 */
public record TransactionParseResult(
    BigDecimal amount,
    String type,
    String behaviorTag,
    String moodTag,
    String remark,
    Double confidence,
    Boolean needsManualReview
) {}
