package com.everycent.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.assistant.memory.AiMemoryService;
import com.everycent.assistant.memory.MemoryRetrievalService;
import com.everycent.assistant.prompt.AssistantPromptBuilder;
import com.everycent.domain.User;
import com.everycent.llm.client.OpenAiCompatibleLlmClient;
import com.everycent.llm.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class AiAssistantSingleTurnIT {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, String> LOCAL_ENV = loadLocalEnv();

    @Test
    @Timeout(120)
    void shouldReplyAsHaoweiWithoutRagMecotOrMemoryPersistence() {
        AiAssistantOrchestrator orchestrator = new AiAssistantOrchestrator(
            null,
            null,
            new AssistantPromptBuilder(),
            llmClient()
        );
        User user = new User();
        user.setId(1L);
        user.setLogin("assistant-single-turn-user");

        ChatRequestDTO request = new ChatRequestDTO();
        request.setMessage("皓尾，我今天有点累，但还是想把晚饭花了28元这件事记下来。");

        ChatResponseDTO response = orchestrator.chat(user, request);

        System.out.println("ASSISTANT ANSWER:");
        System.out.println(response.getAssistantMessage());
        System.out.println("CONVERSATION_ID=" + response.getConversationId());
        System.out.println("MESSAGE_ID=" + response.getMessageId());
        System.out.println("RETRIEVED_MEMORY_COUNT=" + response.getRetrievedMemories().size());

        assertThat(response.getAssistantMessage()).isNotBlank();
        assertThat(response.getAssistantMessage()).contains("本龙");
        assertThat(response.getAssistantMessage()).contains("{\"mood\"");
        assertThat(response.getUserEmotionTagCode()).isEqualTo("NEUTRAL");
        assertThat(response.getAccountingCapture().getCaptured()).isFalse();
        assertThat(response.getRetrievedMemories()).isEmpty();
    }

    private OpenAiCompatibleLlmClient llmClient() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider(env("APP_LLM_PROVIDER", "openai-compatible"));
        properties.setBaseUrl(env("APP_LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"));
        properties.setApiKey(env("APP_LLM_API_KEY", ""));
        properties.setModel(env("APP_LLM_MODEL", "qwen3.6-flash"));
        properties.setTimeoutSeconds(Math.max(60, Integer.parseInt(env("APP_LLM_TIMEOUT_SECONDS", "60"))));
        properties.setMaxTokens(Integer.parseInt(env("APP_LLM_MAX_TOKENS", "160")));
        properties.setTemperature(Double.parseDouble(env("APP_LLM_TEMPERATURE", "0.75")));
        properties.setTopP(Double.parseDouble(env("APP_LLM_TOP_P", "0.85")));
        properties.setRandomizeSampling(Boolean.parseBoolean(env("APP_LLM_RANDOMIZE_SAMPLING", "false")));
        properties.setMinTemperature(Double.parseDouble(env("APP_LLM_MIN_TEMPERATURE", "0.75")));
        properties.setMaxTemperature(Double.parseDouble(env("APP_LLM_MAX_TEMPERATURE", "0.75")));
        properties.setMinTopP(Double.parseDouble(env("APP_LLM_MIN_TOP_P", "0.85")));
        properties.setMaxTopP(Double.parseDouble(env("APP_LLM_MAX_TOP_P", "0.85")));
        return new OpenAiCompatibleLlmClient(properties, OBJECT_MAPPER);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = LOCAL_ENV.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Map<String, String> loadLocalEnv() {
        java.nio.file.Path path = java.nio.file.Path.of("src/main/docker/everycent.env");
        Map<String, String> values = new LinkedHashMap<>();
        if (!java.nio.file.Files.isRegularFile(path)) {
            return values;
        }
        try (BufferedReader reader = java.nio.file.Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                values.put(trimmed.substring(0, separator), trimmed.substring(separator + 1));
            }
        } catch (IOException ignored) {
            // Environment variables remain the source of truth when the local env file cannot be read.
        }
        return values;
    }
}
