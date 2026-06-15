package com.everycent.llm;

/**
 * Builds prompts for EveryCent AI-assisted accounting features.
 * TODO: Add prompt templates for transaction parsing and budget warnings.
 */
public interface LlmPromptService {
    String buildTransactionParsingPrompt(String naturalLanguageText);
}
