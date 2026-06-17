package com.everycent.assistant.memory;

import com.everycent.config.EmbeddingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingClient.class);
    private static final String EMBEDDINGS_PATH = "/embeddings";

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final Duration timeout;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleEmbeddingClient(EmbeddingProperties properties, ObjectMapper objectMapper) {
        this.baseUrl = trimToEmpty(properties.getBaseUrl());
        this.apiKey = trimToEmpty(properties.getApiKey());
        this.model = trimToEmpty(properties.getModel());
        this.dimension = properties.getDimension() == null ? 0 : properties.getDimension();
        this.timeout = Duration.ofSeconds(properties.getTimeoutSeconds() == null ? 0 : properties.getTimeoutSeconds());
        this.restTemplate = buildRestTemplate(timeout);
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] embed(String text) {
        List<float[]> vectors = embedBatch(List.of(text));
        return vectors.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        validateConfiguration();
        if (texts == null || texts.isEmpty() || texts.stream().anyMatch(text -> !StringUtils.hasText(text))) {
            throw new EmbeddingClientException("Embedding 输入文本不能为空");
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                resolveEmbeddingsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(buildRequestBody(texts), buildHeaders()),
                String.class
            );
            return extractVectors(response.getBody(), texts.size());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.warn("Embedding authentication failed with HTTP status {}", e.getStatusCode().value());
            throw new EmbeddingClientException("Embedding 认证失败，请检查 API Key 配置", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Embedding rate limit exceeded with HTTP status {}", e.getStatusCode().value());
            throw new EmbeddingClientException("Embedding 调用触发限流，请稍后重试", e);
        } catch (HttpClientErrorException e) {
            log.warn("Embedding client request failed with HTTP status {}", e.getStatusCode().value());
            throw new EmbeddingClientException("Embedding 请求失败", e);
        } catch (HttpServerErrorException e) {
            log.warn("Embedding server request failed with HTTP status {}", e.getStatusCode().value());
            throw new EmbeddingClientException("Embedding 服务暂时不可用", e);
        } catch (ResourceAccessException e) {
            log.warn("Embedding network or timeout error: {}", e.getMessage());
            throw new EmbeddingClientException("Embedding 网络超时或不可达", e);
        } catch (RestClientException e) {
            log.warn("Embedding REST call failed: {}", e.getMessage());
            throw new EmbeddingClientException("Embedding 调用失败", e);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(baseUrl)) {
            throw new EmbeddingClientException("Embedding baseUrl 未配置");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new EmbeddingClientException("Embedding API Key 未配置");
        }
        if (!StringUtils.hasText(model)) {
            throw new EmbeddingClientException("Embedding model 未配置");
        }
        if (dimension <= 0) {
            throw new EmbeddingClientException("Embedding dimension 配置无效");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new EmbeddingClientException("Embedding timeout 配置无效");
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private Map<String, Object> buildRequestBody(List<String> texts) {
        Object input = texts.size() == 1 ? texts.get(0) : texts;
        return Map.of("model", model, "input", input, "dimensions", dimension);
    }

    private List<float[]> extractVectors(String responseBody, int expectedCount) {
        if (!StringUtils.hasText(responseBody)) {
            throw new EmbeddingClientException("Embedding 返回内容为空");
        }

        try {
            JsonNode data = objectMapper.readTree(responseBody).path("data");
            if (!data.isArray() || data.size() != expectedCount) {
                throw new EmbeddingClientException("Embedding 返回向量数量不符合预期");
            }

            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.path("embedding");
                if (!embedding.isArray() || embedding.size() != dimension) {
                    throw new EmbeddingClientException("Embedding 返回向量维度不符合配置");
                }
                float[] vector = new float[dimension];
                for (int i = 0; i < dimension; i++) {
                    vector[i] = (float) embedding.get(i).asDouble();
                }
                vectors.add(vector);
            }
            return vectors;
        } catch (EmbeddingClientException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingClientException("Embedding 响应解析失败", e);
        }
    }

    private String resolveEmbeddingsUrl() {
        if (baseUrl.endsWith(EMBEDDINGS_PATH)) {
            return baseUrl;
        }
        return baseUrl.replaceAll("/+$", "") + EMBEDDINGS_PATH;
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
}
