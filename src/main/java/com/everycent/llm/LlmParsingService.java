package com.everycent.llm;

import com.everycent.llm.dto.TransactionParseResult;

/**
 * Parses and validates LLM output before it can be used by business services.
 * TODO: Enforce amount, type, behavior tag and mood tag validation rules.
 */
public interface LlmParsingService {
    TransactionParseResult parseTransactionText(String naturalLanguageText);
}
