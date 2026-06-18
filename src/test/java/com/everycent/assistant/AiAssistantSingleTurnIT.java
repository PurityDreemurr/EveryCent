package com.everycent.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.assistant.memory.AiMemoryService;
import com.everycent.assistant.memory.MemoryRetrievalService;
import com.everycent.assistant.memory.OpenAiCompatibleEmbeddingClient;
import com.everycent.assistant.memory.QdrantVectorStoreClient;
import com.everycent.assistant.prompt.AssistantPromptBuilder;
import com.everycent.config.EmbeddingProperties;
import com.everycent.config.VectorStoreProperties;
import com.everycent.domain.User;
import com.everycent.llm.client.OpenAiCompatibleLlmClient;
import com.everycent.llm.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class AiAssistantSingleTurnIT {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final Map<String, String> LOCAL_ENV = loadLocalEnv();
    private static final int EXPECTED_DIMENSION = 1024;

    @Test
    @Timeout(120)
    void shouldReplyAsHaoweiAndPersistConversationMemory() {
        AiAssistantOrchestrator orchestrator = new AiAssistantOrchestrator(
            memoryRetrievalService(),
            aiMemoryService(),
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
    }

    private AiMemoryService aiMemoryService() {
        return new AiMemoryService(embeddingClient(), vectorStoreClient());
    }

    private MemoryRetrievalService memoryRetrievalService() {
        return new MemoryRetrievalService(embeddingClient(), vectorStoreClient());
    }

    private OpenAiCompatibleEmbeddingClient embeddingClient() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setProvider(env("APP_EMBEDDING_PROVIDER", "openai-compatible"));
        properties.setBaseUrl(env("APP_EMBEDDING_BASE_URL", ""));
        properties.setApiKey(env("APP_EMBEDDING_API_KEY", ""));
        properties.setModel(env("APP_EMBEDDING_MODEL", "text-embedding-v3"));
        properties.setDimension(Integer.parseInt(env("APP_EMBEDDING_DIMENSION", Integer.toString(EXPECTED_DIMENSION))));
        properties.setTimeoutSeconds(Integer.parseInt(env("APP_EMBEDDING_TIMEOUT_SECONDS", "20")));
        return new OpenAiCompatibleEmbeddingClient(properties, OBJECT_MAPPER);
    }

    private QdrantVectorStoreClient vectorStoreClient() {
        VectorStoreProperties properties = new VectorStoreProperties();
        properties.setProvider(env("APP_VECTOR_STORE_PROVIDER", "qdrant"));
        properties.setBaseUrl(resolveVectorStoreBaseUrl());
        properties.setMemoryCollection(env("APP_VECTOR_STORE_MEMORY_COLLECTION", "everycent_ai_memory"));
        properties.setRoleKnowledgeCollection(env("APP_VECTOR_STORE_ROLE_KNOWLEDGE_COLLECTION", "everycent_ai_role_knowledge"));
        properties.setTimeoutSeconds(Integer.parseInt(env("APP_VECTOR_STORE_TIMEOUT_SECONDS", "10")));
        return new QdrantVectorStoreClient(properties, OBJECT_MAPPER);
    }

    private OpenAiCompatibleLlmClient llmClient() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider(env("APP_LLM_PROVIDER", "openai-compatible"));
        properties.setBaseUrl(env("APP_LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"));
        properties.setApiKey(env("APP_LLM_API_KEY", ""));
        properties.setModel(env("APP_LLM_MODEL", "qwen3.6-flash"));
        properties.setTimeoutSeconds(Math.max(60, Integer.parseInt(env("APP_LLM_TIMEOUT_SECONDS", "60"))));
        properties.setMaxTokens(Integer.parseInt(env("APP_LLM_MAX_TOKENS", "160")));
        properties.setTemperature(Double.parseDouble(env("APP_LLM_TEMPERATURE", "0.9")));
        properties.setTopP(Double.parseDouble(env("APP_LLM_TOP_P", "0.95")));
        properties.setRandomizeSampling(Boolean.parseBoolean(env("APP_LLM_RANDOMIZE_SAMPLING", "true")));
        properties.setMinTemperature(Double.parseDouble(env("APP_LLM_MIN_TEMPERATURE", "0.85")));
        properties.setMaxTemperature(Double.parseDouble(env("APP_LLM_MAX_TEMPERATURE", "1.25")));
        properties.setMinTopP(Double.parseDouble(env("APP_LLM_MIN_TOP_P", "0.90")));
        properties.setMaxTopP(Double.parseDouble(env("APP_LLM_MAX_TOP_P", "0.98")));
        return new OpenAiCompatibleLlmClient(properties, OBJECT_MAPPER);
    }

    private static String resolveVectorStoreBaseUrl() {
        List<String> candidates = Arrays.asList(
            env("DEV_VECTOR_STORE_BASE_URL", ""),
            env("APP_VECTOR_STORE_BASE_URL", ""),
            "http://host.docker.internal:6333",
            "http://" + defaultGateway() + ":6333",
            "http://127.0.0.1:6333"
        );
        for (String candidate : candidates) {
            if (!candidate.isBlank() && isReachable(candidate)) {
                System.out.println("Resolved Qdrant baseUrl=" + candidate);
                return candidate;
            }
        }
        return env("DEV_VECTOR_STORE_BASE_URL", env("APP_VECTOR_STORE_BASE_URL", "http://127.0.0.1:6333"));
    }

    private static boolean isReachable(String baseUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(baseUrl) + "/healthz"))
                .timeout(java.time.Duration.ofSeconds(3))
                .GET()
                .build();
            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private static String defaultGateway() {
        try (
            BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(
                    java.nio.file.Files.newInputStream(java.nio.file.Path.of("/proc/net/route")),
                    StandardCharsets.UTF_8
                )
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.trim().split("\\s+");
                if (fields.length > 2 && "00000000".equals(fields[1])) {
                    long gatewayHex = Long.parseLong(fields[2], 16);
                    return String.format(
                        "%d.%d.%d.%d",
                        gatewayHex & 0xff,
                        (gatewayHex >> 8) & 0xff,
                        (gatewayHex >> 16) & 0xff,
                        (gatewayHex >> 24) & 0xff
                    );
                }
            }
        } catch (Exception ignored) {
            // Fall through to localhost.
        }
        return "127.0.0.1";
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

    private static String trimTrailingSlash(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }
}
