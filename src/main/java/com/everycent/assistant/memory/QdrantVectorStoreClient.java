package com.everycent.assistant.memory;

import com.everycent.config.VectorStoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class QdrantVectorStoreClient implements VectorStoreClient {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreClient.class);

    private final String baseUrl;
    private final String memoryCollection;
    private final String roleKnowledgeCollection;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public QdrantVectorStoreClient(VectorStoreProperties properties, ObjectMapper objectMapper) {
        this.baseUrl = trimTrailingSlash(properties.getBaseUrl());
        this.memoryCollection = trimToEmpty(properties.getMemoryCollection());
        this.roleKnowledgeCollection = trimToEmpty(properties.getRoleKnowledgeCollection());
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds() == null ? 0 : properties.getTimeoutSeconds());
        this.restTemplate = buildRestTemplate(timeout);
        this.objectMapper = objectMapper;
    }

    @Override
    public String upsertMemory(Long userId, Long memoryId, float[] vector, Map<String, Object> payload) {
        requireId(userId, "userId");
        requireId(memoryId, "memoryId");
        Map<String, Object> safePayload = payloadWith(payload, Map.of("userId", userId, "memoryId", memoryId));
        String vectorId = stableUuid("memory:" + userId + ":" + memoryId);
        upsert(memoryCollection, vectorId, vector, safePayload);
        return vectorId;
    }

    @Override
    public String upsertRoleKnowledge(Long roleProfileId, Long roleKnowledgeId, float[] vector, Map<String, Object> payload) {
        requireId(roleProfileId, "roleProfileId");
        requireId(roleKnowledgeId, "roleKnowledgeId");
        Map<String, Object> safePayload = payloadWith(
            payload,
            Map.of("roleProfileId", roleProfileId, "roleKnowledgeId", roleKnowledgeId, "enabled", true)
        );
        String vectorId = stableUuid("role:" + roleProfileId + ":" + roleKnowledgeId);
        upsert(roleKnowledgeCollection, vectorId, vector, safePayload);
        return vectorId;
    }

    @Override
    public List<VectorSearchResult> searchMemory(Long userId, float[] queryVector, int limit) {
        requireId(userId, "userId");
        Map<String, Object> filter = mustFilter(Map.of("userId", userId));
        return search(memoryCollection, queryVector, filter, limit);
    }

    @Override
    public List<VectorSearchResult> searchRoleKnowledge(Long roleProfileId, float[] queryVector, int limit) {
        requireId(roleProfileId, "roleProfileId");
        Map<String, Object> filter = mustFilter(Map.of("roleProfileId", roleProfileId, "enabled", true));
        return search(roleKnowledgeCollection, queryVector, filter, limit);
    }

    public List<VectorSearchResult> scrollRoleKnowledge(Long roleProfileId, int limit) {
        requireId(roleProfileId, "roleProfileId");
        if (limit <= 0) {
            throw new VectorStoreException("向量滚动读取 limit 必须大于 0");
        }
        Map<String, Object> body = Map.of(
            "filter",
            mustFilter(Map.of("roleProfileId", roleProfileId, "enabled", true)),
            "limit",
            limit,
            "with_payload",
            true,
            "with_vector",
            false
        );
        JsonNode response = exchange(HttpMethod.POST, "/collections/" + roleKnowledgeCollection + "/points/scroll", body);
        List<VectorSearchResult> results = new ArrayList<>();
        for (JsonNode item : response.path("result").path("points")) {
            VectorSearchResult result = new VectorSearchResult();
            result.setVectorId(item.path("id").asText());
            result.setPayload(toMap(item.path("payload")));
            results.add(result);
        }
        return results;
    }

    @Override
    public void delete(String vectorId) {
        if (!StringUtils.hasText(vectorId)) {
            throw new VectorStoreException("vectorId 不能为空");
        }
        delete(memoryCollection, vectorId);
        delete(roleKnowledgeCollection, vectorId);
    }

    private void upsert(String collection, String vectorId, float[] vector, Map<String, Object> payload) {
        validateConfiguration();
        validateVector(vector);
        Map<String, Object> point = Map.of("id", vectorId, "vector", vector, "payload", payload);
        Map<String, Object> body = Map.of("points", List.of(point));
        exchange(HttpMethod.PUT, "/collections/" + collection + "/points?wait=true", body);
    }

    private List<VectorSearchResult> search(String collection, float[] queryVector, Map<String, Object> filter, int limit) {
        validateConfiguration();
        validateVector(queryVector);
        if (limit <= 0) {
            throw new VectorStoreException("向量检索 limit 必须大于 0");
        }
        Map<String, Object> body = Map.of("vector", queryVector, "filter", filter, "limit", limit, "with_payload", true);
        JsonNode response = exchange(HttpMethod.POST, "/collections/" + collection + "/points/search", body);
        List<VectorSearchResult> results = new ArrayList<>();
        for (JsonNode item : response.path("result")) {
            VectorSearchResult result = new VectorSearchResult();
            result.setVectorId(item.path("id").asText());
            result.setScore(item.path("score").asDouble());
            result.setPayload(toMap(item.path("payload")));
            results.add(result);
        }
        return results;
    }

    private void delete(String collection, String vectorId) {
        Map<String, Object> body = Map.of("points", List.of(vectorId));
        exchange(HttpMethod.POST, "/collections/" + collection + "/points/delete?wait=true", body);
    }

    private JsonNode exchange(HttpMethod method, String path, Map<String, Object> body) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + path,
                method,
                new HttpEntity<>(objectMapper.writeValueAsBytes(body), headers()),
                String.class
            );
            return objectMapper.readTree(response.getBody());
        } catch (HttpServerErrorException e) {
            log.warn("Qdrant server error with HTTP status {}", e.getStatusCode().value());
            throw new VectorStoreException("Qdrant 服务暂时不可用", e);
        } catch (ResourceAccessException e) {
            log.warn("Qdrant network or timeout error: {}", e.getMessage());
            throw new VectorStoreException("Qdrant 网络超时或不可达", e);
        } catch (JsonProcessingException e) {
            throw new VectorStoreException("Qdrant 请求或响应 JSON 处理失败", e);
        } catch (RestClientException e) {
            log.warn("Qdrant REST call failed: {}", e.getMessage());
            throw new VectorStoreException("Qdrant 调用失败", e);
        }
    }

    private Map<String, Object> mustFilter(Map<String, Object> matches) {
        List<Map<String, Object>> must = matches
            .entrySet()
            .stream()
            .map(entry -> Map.of("key", entry.getKey(), "match", Map.of("value", entry.getValue())))
            .toList();
        return Map.of("must", must);
    }

    private Map<String, Object> payloadWith(Map<String, Object> payload, Map<String, Object> enforcedValues) {
        Map<String, Object> safePayload = new LinkedHashMap<>();
        if (payload != null) {
            safePayload.putAll(payload);
        }
        safePayload.putAll(enforcedValues);
        return safePayload;
    }

    private Map<String, Object> toMap(JsonNode payload) {
        return objectMapper.convertValue(payload, new TypeReference<>() {});
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(baseUrl)) {
            throw new VectorStoreException("Qdrant baseUrl 未配置");
        }
        if (!StringUtils.hasText(memoryCollection)) {
            throw new VectorStoreException("Qdrant memory collection 未配置");
        }
        if (!StringUtils.hasText(roleKnowledgeCollection)) {
            throw new VectorStoreException("Qdrant role knowledge collection 未配置");
        }
    }

    private void validateVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new VectorStoreException("向量不能为空");
        }
    }

    private void requireId(Long id, String fieldName) {
        if (Objects.isNull(id)) {
            throw new VectorStoreException(fieldName + " 不能为空");
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private static RestTemplate buildRestTemplate(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }

    private static String trimTrailingSlash(String value) {
        return trimToEmpty(value).replaceAll("/+$", "");
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stableUuid(String source) {
        return UUID.nameUUIDFromBytes(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }
}
