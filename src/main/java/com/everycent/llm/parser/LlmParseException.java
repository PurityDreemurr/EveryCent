package com.everycent.llm.parser;

public class LlmParseException extends RuntimeException {

    public LlmParseException(String message) {
        super(message);
    }

    public LlmParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
