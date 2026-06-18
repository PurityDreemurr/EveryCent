package com.everycent.assistant.emotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.assistant.dto.MemoryContextDTO;
import com.everycent.assistant.prompt.AssistantPromptBuilder;
import com.everycent.domain.AiEmotionState;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.enumeration.EmotionValence;
import com.everycent.llm.client.OpenAiCompatibleLlmClient;
import com.everycent.llm.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class MecotEmotionThreeTurnIT {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, String> LOCAL_ENV = loadLocalEnv();

    @Test
    @Timeout(180)
    void shouldRunThreeTurnsWithMecotTransitionsAndLlmAnswers() {
        MecotEmotionService mecotEmotionService = new MecotEmotionService();
        AiEmotionPromptAdapter promptAdapter = new AiEmotionPromptAdapter();
        AssistantPromptBuilder promptBuilder = new AssistantPromptBuilder();
        OpenAiCompatibleLlmClient llmClient = llmClient();

        List<Turn> turns = List.of(
            new Turn("STRESSED", EmotionValence.NEGATIVE, "皓尾，我今天加班好累，晚饭花了28元。"),
            new Turn("REGRET", EmotionValence.NEGATIVE, "我有点后悔，下午又买了杯奶茶，18元。"),
            new Turn("HAPPY", EmotionValence.POSITIVE, "不过刚才收到报销120元，心情好一点了。")
        );

        AiEmotionState currentState = AiEmotionStateModel.defaultState();
        StringBuilder path = new StringBuilder(currentState.getCurrentEmotion());
        for (int i = 0; i < turns.size(); i++) {
            Turn turn = turns.get(i);
            EmotionTag userEmotionTag = emotionTag(turn.emotionCode(), turn.valence());
            AiEmotionTransitionResult transition = mecotEmotionService.transition(currentState, userEmotionTag, turn.userInput(), List.of());
            String styleInstruction = promptAdapter.buildStyleInstruction(currentState, transition);
            String prompt = promptBuilder.buildSingleTurnPrompt(turn.userInput(), turn.emotionCode(), transition, styleInstruction, List.of());
            String answer = llmClient.complete(prompt);

            path.append(" -> ").append(transition.getAfterEmotion());
            System.out.printf(
                "TURN %d INPUT: %s%nMECOT: %s -> %s, userEmotion=%s, strategy=%s, delta=(%s,%s), top=%s, reason=%s%nLLM ANSWER:%n%s%n%n",
                i + 1,
                turn.userInput(),
                transition.getBeforeEmotion(),
                transition.getAfterEmotion(),
                transition.getUserEmotionTagCode(),
                transition.getSelectionStrategy(),
                transition.getRationalDeltaValence(),
                transition.getRationalDeltaArousal(),
                transition.getTopCandidates(),
                transition.getReason(),
                answer
            );
            currentState = AiEmotionStateModel.state(transition.getAfterEmotion());
            assertThat(answer).isNotBlank();
            assertThat(transition.getAfterEmotion()).isNotBlank();
            assertThat(transition.getSelectionStrategy()).isNotNull();
            assertThat(transition.getTopCandidates()).isNotEmpty();
        }
        System.out.println("MECOT STATE PATH: " + path);
    }

    private EmotionTag emotionTag(String code, EmotionValence valence) {
        EmotionTag tag = new EmotionTag();
        tag.setCode(code);
        tag.setName(code);
        tag.setValence(valence);
        tag.setSystemDefault(true);
        return tag;
    }

    private OpenAiCompatibleLlmClient llmClient() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider(env("APP_LLM_PROVIDER", "openai-compatible"));
        properties.setBaseUrl(env("APP_LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"));
        properties.setApiKey(env("APP_LLM_API_KEY", ""));
        properties.setModel(env("APP_LLM_MODEL", "qwen3.6-flash"));
        properties.setTimeoutSeconds(Math.max(60, Integer.parseInt(env("APP_LLM_TIMEOUT_SECONDS", "60"))));
        properties.setMaxTokens(Integer.parseInt(env("APP_LLM_MAX_TOKENS", "140")));
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

    private record Turn(String emotionCode, EmotionValence valence, String userInput) {}
}
