package com.everycent.assistant.rewrite;

import com.everycent.assistant.validation.DialogueScene;
import com.everycent.llm.client.OpenAiCompatibleLlmClient;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LlmRewriteService {

    private static final double REWRITE_TEMPERATURE = 0.3;
    private static final double REWRITE_TOP_P = 0.85;

    private final RewritePromptBuilder rewritePromptBuilder;
    private final OpenAiCompatibleLlmClient llmClient;

    public LlmRewriteService(RewritePromptBuilder rewritePromptBuilder, OpenAiCompatibleLlmClient llmClient) {
        this.rewritePromptBuilder = rewritePromptBuilder;
        this.llmClient = llmClient;
    }

    public String rewrite(String userMessage, String badReply, DialogueScene scene, List<String> violations) {
        return llmClient.completeRaw(rewritePromptBuilder.buildRewritePrompt(userMessage, badReply, scene, violations), REWRITE_TEMPERATURE, REWRITE_TOP_P);
    }
}
