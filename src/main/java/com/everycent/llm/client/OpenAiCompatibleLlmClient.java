package com.everycent.llm.client;

import com.everycent.llm.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String TEST_SYSTEM_PROMPT = "你是 EveryCent 的测试助手。请根据用户输入返回简洁中文回复。";

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final double temperature;
    private final double topP;
    private final int maxTokens;
    private final boolean randomizeSampling;
    private final double minTemperature;
    private final double maxTemperature;
    private final double minTopP;
    private final double maxTopP;
    private final boolean enableThinking;
    private final boolean enableSearch;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.baseUrl = trimToEmpty(properties.getBaseUrl());
        this.apiKey = trimToEmpty(properties.getApiKey());
        this.model = trimToEmpty(properties.getModel());
        this.timeout = Duration.ofSeconds(properties.getTimeoutSeconds() == null ? 0 : properties.getTimeoutSeconds());
        this.temperature = defaultDouble(properties.getTemperature(), 0.75);
        this.topP = defaultDouble(properties.getTopP(), 0.85);
        this.maxTokens = properties.getMaxTokens() == null ? 180 : properties.getMaxTokens();
        this.randomizeSampling = Boolean.TRUE.equals(properties.getRandomizeSampling());
        this.minTemperature = defaultDouble(properties.getMinTemperature(), 0.75);
        this.maxTemperature = defaultDouble(properties.getMaxTemperature(), 0.75);
        this.minTopP = defaultDouble(properties.getMinTopP(), 0.85);
        this.maxTopP = defaultDouble(properties.getMaxTopP(), 0.85);
        this.enableThinking = Boolean.TRUE.equals(properties.getEnableThinking());
        this.enableSearch = Boolean.TRUE.equals(properties.getEnableSearch());
        this.restTemplate = buildRestTemplate(timeout);
        this.objectMapper = objectMapper;
    }

    @Override
    public String complete(String prompt) {
        return completeWithMessages(List.of(Map.of("role", "system", "content", TEST_SYSTEM_PROMPT), Map.of("role", "user", "content", prompt)), null, null);
    }

    public String completeRaw(String prompt) {
        return completeWithMessages(List.of(Map.of("role", "user", "content", prompt)), null, null);
    }

    public String completeRaw(String prompt, double temperature, double topP) {
        return completeWithMessages(List.of(Map.of("role", "user", "content", prompt)), temperature, topP);
    }

    private String completeWithMessages(List<Map<String, String>> messages, Double temperatureOverride, Double topPOverride) {
        validateConfiguration();
        if (messages == null || messages.isEmpty() || messages.stream().noneMatch(message -> StringUtils.hasText(message.get("content")))) {
            throw new LlmClientException("LLM prompt 不能为空，请使用手动记账");
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                resolveChatCompletionsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(buildRequestBody(messages, temperatureOverride, topPOverride), buildHeaders()),
                String.class
            );
            return extractContent(response.getBody());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.warn("LLM authentication failed with HTTP status {}", e.getStatusCode().value());
            throw new LlmClientException("LLM 认证失败，请检查 API Key 配置", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("LLM rate limit exceeded with HTTP status {}", e.getStatusCode().value());
            throw new LlmClientException("LLM 调用触发限流，请稍后重试或使用手动记账", e);
        } catch (HttpClientErrorException e) {
            log.warn("LLM client request failed with HTTP status {}", e.getStatusCode().value());
            throw new LlmClientException("LLM 请求失败，请使用手动记账", e);
        } catch (HttpServerErrorException e) {
            log.warn("LLM server request failed with HTTP status {}", e.getStatusCode().value());
            throw new LlmClientException("LLM 服务暂时不可用，请使用手动记账", e);
        } catch (ResourceAccessException e) {
            log.warn("LLM network or timeout error: {}", e.getMessage());
            throw new LlmClientException("LLM 网络超时或不可达，请使用手动记账", e);
        } catch (RestClientException e) {
            log.warn("LLM REST call failed: {}", e.getMessage());
            throw new LlmClientException("LLM 调用失败，请使用手动记账", e);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(baseUrl)) {
            throw new LlmClientException("LLM baseUrl 未配置，请使用手动记账");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new LlmClientException("LLM API Key 未配置，请使用手动记账");
        }
        if (!StringUtils.hasText(model)) {
            throw new LlmClientException("LLM model 未配置，请使用手动记账");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new LlmClientException("LLM timeout 配置无效，请使用手动记账");
        }
        if (randomizeSampling && (minTemperature > maxTemperature || minTopP > maxTopP)) {
            throw new LlmClientException("LLM 随机采样范围配置无效，请使用手动记账");
        }
        if (maxTokens <= 0) {
            throw new LlmClientException("LLM maxTokens 配置无效，请使用手动记账");
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private Map<String, Object> buildRequestBody(List<Map<String, String>> messages, Double temperatureOverride, Double topPOverride) {
        double resolvedTemperature = temperatureOverride == null
            ? randomizeSampling ? randomDouble(minTemperature, maxTemperature) : temperature
            : temperatureOverride;
        double resolvedTopP = topPOverride == null ? randomizeSampling ? randomDouble(minTopP, maxTopP) : topP : topPOverride;
        log.info(
            "LLM request parameters model={}, temperature={}, topP={}, enableThinking={}, enableSearch={}",
            model,
            resolvedTemperature,
            resolvedTopP,
            enableThinking,
            enableSearch
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", resolvedTemperature);
        body.put("top_p", resolvedTopP);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        body.put("enable_thinking", enableThinking);
        body.put("enable_search", enableSearch);
        return body;
    }

    private String extractContent(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new LlmClientException("LLM 返回内容为空，请手动填写");
        }

        try {
            JsonNode content = objectMapper.readTree(responseBody).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || !StringUtils.hasText(content.asText())) {
                throw new LlmClientException("LLM 返回内容为空，请手动填写");
            }
            return content.asText();
        } catch (LlmClientException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmClientException("LLM 响应解析失败，请手动填写", e);
        }
    }

    private String resolveChatCompletionsUrl() {
        if (baseUrl.endsWith(CHAT_COMPLETIONS_PATH)) {
            return baseUrl;
        }
        return baseUrl.replaceAll("/+$", "") + CHAT_COMPLETIONS_PATH;
    }

    private static RestTemplate buildRestTemplate(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static double defaultDouble(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static double randomDouble(double min, double max) {
        if (Double.compare(min, max) == 0) {
            return min;
        }
        return Math.round(ThreadLocalRandom.current().nextDouble(min, max) * 100.0) / 100.0;
    }
}
