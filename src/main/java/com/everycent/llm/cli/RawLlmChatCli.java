package com.everycent.llm.cli;

import com.everycent.llm.client.LlmClientException;
import com.everycent.llm.client.OpenAiCompatibleLlmClient;
import com.everycent.llm.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class RawLlmChatCli {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RawLlmChatCli() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> localEnv = loadLocalEnv();
        OpenAiCompatibleLlmClient llmClient = new OpenAiCompatibleLlmClient(llmProperties(localEnv), OBJECT_MAPPER);

        printBanner();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("\n你> ");
                String input = reader.readLine();
                if (input == null || isExit(input)) {
                    System.out.println("bye");
                    return;
                }
                if (!StringUtils.hasText(input)) {
                    continue;
                }
                callRawLlm(llmClient, input.trim());
            }
        }
    }

    private static void callRawLlm(OpenAiCompatibleLlmClient llmClient, String input) {
        Instant start = Instant.now();
        try {
            String answer = llmClient.completeRaw(input);
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            System.out.println("\nLLM> " + answer.strip());
            System.out.println("[elapsed] " + elapsedMs + " ms");
        } catch (LlmClientException e) {
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            System.out.println("\n[LLM 调用失败] " + e.getMessage());
            if (e.getCause() != null && StringUtils.hasText(e.getCause().getMessage())) {
                System.out.println("[原因] " + e.getCause().getMessage());
            }
            System.out.println("[elapsed] " + elapsedMs + " ms");
        }
    }

    private static LlmProperties llmProperties(Map<String, String> localEnv) {
        LlmProperties properties = new LlmProperties();
        properties.setProvider(env("APP_LLM_PROVIDER", "openai-compatible", localEnv));
        properties.setBaseUrl(env("APP_LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1", localEnv));
        properties.setApiKey(env("APP_LLM_API_KEY", "", localEnv));
        properties.setModel(env("APP_LLM_MODEL", "qwen3.6-flash", localEnv));
        properties.setTimeoutSeconds(parseInt(env("APP_LLM_TIMEOUT_SECONDS", "60", localEnv), 60));
        properties.setMaxTokens(parseInt(env("APP_LLM_MAX_TOKENS", "180", localEnv), 180));
        properties.setTemperature(parseDouble(env("APP_LLM_TEMPERATURE", "0.75", localEnv), 0.75));
        properties.setTopP(parseDouble(env("APP_LLM_TOP_P", "0.85", localEnv), 0.85));
        properties.setRandomizeSampling(Boolean.parseBoolean(env("APP_LLM_RANDOMIZE_SAMPLING", "false", localEnv)));
        properties.setMinTemperature(parseDouble(env("APP_LLM_MIN_TEMPERATURE", "0.75", localEnv), 0.75));
        properties.setMaxTemperature(parseDouble(env("APP_LLM_MAX_TEMPERATURE", "0.75", localEnv), 0.75));
        properties.setMinTopP(parseDouble(env("APP_LLM_MIN_TOP_P", "0.85", localEnv), 0.85));
        properties.setMaxTopP(parseDouble(env("APP_LLM_MAX_TOP_P", "0.85", localEnv), 0.85));
        properties.setEnableThinking(Boolean.parseBoolean(env("APP_LLM_ENABLE_THINKING", "false", localEnv)));
        properties.setEnableSearch(Boolean.parseBoolean(env("APP_LLM_ENABLE_SEARCH", "false", localEnv)));
        return properties;
    }

    private static String env(String name, String defaultValue, Map<String, String> localEnv) {
        String value = System.getenv(name);
        if (StringUtils.hasText(value)) {
            return value;
        }
        value = localEnv.get(name);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static Map<String, String> loadLocalEnv() {
        Path path = Path.of("src/main/docker/everycent.env");
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(path)) {
            return values;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
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

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isExit(String input) {
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        return "/exit".equals(trimmed) || "/quit".equals(trimmed) || "exit".equals(trimmed) || "quit".equals(trimmed);
    }

    private static void printBanner() {
        System.out.println("EveryCent Raw LLM CLI");
        System.out.println("模式：无系统提示词、无 RAG、无 MeCOT、无解析器、无数据库。只发送当前用户输入。");
        System.out.println("命令：/exit 或 /quit 退出。每轮会输出 elapsed 耗时。");
    }
}
