package com.everycent.assistant.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.everycent.assistant.memory.EmbeddingClient;
import com.everycent.assistant.memory.OpenAiCompatibleEmbeddingClient;
import com.everycent.assistant.memory.QdrantVectorStoreClient;
import com.everycent.assistant.memory.VectorSearchResult;
import com.everycent.config.EmbeddingProperties;
import com.everycent.config.VectorStoreProperties;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class RoleKnowledgeImportIT {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper ASCII_OBJECT_MAPPER = new ObjectMapper()
        .configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final Map<String, String> LOCAL_ENV = loadLocalEnv();
    private static final int EXPECTED_DIMENSION = 1024;
    private static final String RESOURCE = "/role-knowledge/haowei-finance-role-knowledge.jsonl";

    private static EmbeddingClient embeddingClient;
    private static QdrantVectorStoreClient vectorStoreClient;
    private static RoleKnowledgeIngestionService ingestionService;

    @BeforeAll
    static void setUpClients() {
        EmbeddingProperties embeddingProperties = new EmbeddingProperties();
        embeddingProperties.setBaseUrl(env("APP_EMBEDDING_BASE_URL", ""));
        embeddingProperties.setApiKey(env("APP_EMBEDDING_API_KEY", ""));
        embeddingProperties.setModel(env("APP_EMBEDDING_MODEL", "text-embedding-v3"));
        embeddingProperties.setDimension(Integer.parseInt(env("APP_EMBEDDING_DIMENSION", Integer.toString(EXPECTED_DIMENSION))));
        embeddingProperties.setTimeoutSeconds(Integer.parseInt(env("APP_EMBEDDING_TIMEOUT_SECONDS", "20")));
        embeddingClient = new OpenAiCompatibleEmbeddingClient(embeddingProperties, OBJECT_MAPPER);

        VectorStoreProperties vectorStoreProperties = new VectorStoreProperties();
        vectorStoreProperties.setBaseUrl(resolveVectorStoreBaseUrl());
        vectorStoreProperties.setRoleKnowledgeCollection(
            env("APP_VECTOR_STORE_ROLE_KNOWLEDGE_COLLECTION", "everycent_ai_role_knowledge")
        );
        vectorStoreProperties.setMemoryCollection(env("APP_VECTOR_STORE_MEMORY_COLLECTION", "everycent_ai_memory"));
        vectorStoreProperties.setTimeoutSeconds(Integer.parseInt(env("APP_VECTOR_STORE_TIMEOUT_SECONDS", "10")));
        vectorStoreClient = new QdrantVectorStoreClient(vectorStoreProperties, OBJECT_MAPPER);
        ingestionService = new RoleKnowledgeIngestionService(embeddingClient, vectorStoreClient);

        assumeTrue(true, "clients ready");
    }

    @Test
    @Timeout(180)
    void shouldImportHaoweiFinanceRoleKnowledgeJsonlIntoQdrant() throws Exception {
        List<JsonNode> records = readJsonl(RESOURCE);
        assertThat(records).isNotEmpty();

        int imported = 0;
        for (JsonNode record : records) {
            String id = record.path("id").asText();
            String roleCode = record.path("roleCode").asText();
            String title = record.path("title").asText();
            String knowledgeType = record.path("knowledgeType").asText();
            String content = record.path("content").asText();
            int priority = record.path("priority").asInt();
            boolean enabled = record.path("enabled").asBoolean();

            long roleProfileId = 1L;
            long roleKnowledgeId = Math.abs(id.hashCode());
            String vectorId = ingestionService.saveRoleKnowledgeVector(
                roleProfileId,
                roleKnowledgeId,
                content,
                Map.of(
                    "roleCode",
                    roleCode,
                    "title",
                    title,
                    "content",
                    content,
                    "knowledgeType",
                    knowledgeType,
                    "priority",
                    priority,
                    "enabled",
                    enabled,
                    "createdDate",
                    Instant.now().toString()
                )
            );
            System.out.printf(
                "IMPORTED vectorId=%s sourceId=%s roleCode=%s type=%s priority=%d title=%s%n",
                vectorId,
                id,
                roleCode,
                knowledgeType,
                priority,
                title
            );
            imported++;
        }

        System.out.printf("IMPORT SUMMARY resource=%s imported=%d%n", RESOURCE, imported);
        assertThat(imported).isEqualTo(records.size());

        float[] queryVector = embeddingClient.embed("皓尾是谁，为什么能做财务助手");
        assertThat(queryVector).hasSize(EXPECTED_DIMENSION);
        List<VectorSearchResult> results = vectorStoreClient.searchRoleKnowledge(1L, queryVector, 5);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getPayload()).containsEntry("roleCode", "haowei");

        System.out.println("SEARCH TOP RESULTS:");
        for (VectorSearchResult result : results) {
            System.out.printf(
                "score=%.6f vectorId=%s payload=%s payloadEscaped=%s%n",
                result.getScore(),
                result.getVectorId(),
                result.getPayload(),
                ASCII_OBJECT_MAPPER.writeValueAsString(result.getPayload())
            );
        }

        List<VectorSearchResult> stored = vectorStoreClient.scrollRoleKnowledge(1L, Math.max(imported, 1));
        System.out.printf("STORED ROLE KNOWLEDGE COUNT roleProfileId=1 count=%d%n", stored.size());
        for (VectorSearchResult result : stored) {
            System.out.printf(
                "STORED vectorId=%s payload=%s payloadEscaped=%s%n",
                result.getVectorId(),
                result.getPayload(),
                ASCII_OBJECT_MAPPER.writeValueAsString(result.getPayload())
            );
        }
        assertThat(stored).hasSizeGreaterThanOrEqualTo(imported);
    }

    private static List<JsonNode> readJsonl(String resourcePath) throws Exception {
        List<JsonNode> records = new ArrayList<>();
        try (InputStream inputStream = RoleKnowledgeImportIT.class.getResourceAsStream(resourcePath)) {
            assertThat(inputStream).as("Missing resource " + resourcePath).isNotNull();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    records.add(OBJECT_MAPPER.readTree(line));
                }
            }
        }
        return records;
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = LOCAL_ENV.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(java.nio.file.Files.newInputStream(java.nio.file.Path.of("/proc/net/route")), StandardCharsets.UTF_8))) {
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

    private static String trimTrailingSlash(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
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
