package com.everycent.assistant.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QdrantVectorStoreIT {

    private static final String BASE_URL = System.getenv().getOrDefault("QDRANT_TEST_BASE_URL", "http://127.0.0.1:6333");
    private static final String MEMORY_COLLECTION = System.getenv().getOrDefault("QDRANT_MEMORY_COLLECTION", "everycent_ai_memory");
    private static final String ROLE_COLLECTION = System.getenv().getOrDefault(
        "QDRANT_ROLE_KNOWLEDGE_COLLECTION",
        "everycent_ai_role_knowledge"
    );
    private static final int VECTOR_SIZE = 1024;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll
    static void requireQdrant() throws Exception {
        HttpResponse<String> response = send("GET", "/healthz", null);
        assumeTrue(response.statusCode() == 200, "Qdrant is not running at " + BASE_URL);
    }

    @Test
    void shouldExposeConfiguredCollectionsWithExpectedVectorStructure() throws Exception {
        assertCollectionStructure(MEMORY_COLLECTION);
        assertCollectionStructure(ROLE_COLLECTION);
    }

    @Test
    void shouldWriteSearchAndDeleteMemoryVectorWithUserFilter() throws Exception {
        String pointId = "100001";
        upsertPoint(
            MEMORY_COLLECTION,
            pointId,
            """
            {
              "memoryId": 123,
              "userId": 1,
              "conversationId": 9,
              "messageId": 88,
              "memoryType": "MESSAGE",
              "userEmotionTagCode": "STRESSED",
              "importanceScore": 0.82,
              "createdDate": "2026-06-17T21:30:00Z"
            }
            """
        );

        JsonNode searchResult = search(
            MEMORY_COLLECTION,
            """
            {
              "must": [
                { "key": "userId", "match": { "value": 1 } }
              ]
            }
            """
        );

        JsonNode first = searchResult.path("result").get(0);
        assertThat(first.path("id").asText()).isEqualTo(pointId);
        assertThat(first.path("payload").path("memoryId").asLong()).isEqualTo(123L);
        assertThat(first.path("payload").path("memoryType").asText()).isEqualTo("MESSAGE");
        assertThat(first.path("payload").path("userEmotionTagCode").asText()).isEqualTo("STRESSED");

        deletePoint(MEMORY_COLLECTION, pointId);
    }

    @Test
    void shouldWriteSearchAndDeleteRoleKnowledgeVectorWithRoleFilter() throws Exception {
        String pointId = "200001";
        upsertPoint(
            ROLE_COLLECTION,
            pointId,
            """
            {
              "roleProfileId": 1,
              "roleCode": "FINANCIAL_COMPANION",
              "roleKnowledgeId": 22,
              "knowledgeType": "PERSONALITY",
              "priority": 80,
              "enabled": true,
              "createdDate": "2026-06-17T21:30:00Z"
            }
            """
        );

        JsonNode searchResult = search(
            ROLE_COLLECTION,
            """
            {
              "must": [
                { "key": "roleProfileId", "match": { "value": 1 } },
                { "key": "enabled", "match": { "value": true } }
              ]
            }
            """
        );

        JsonNode first = searchResult.path("result").get(0);
        assertThat(first.path("id").asText()).isEqualTo(pointId);
        assertThat(first.path("payload").path("roleCode").asText()).isEqualTo("FINANCIAL_COMPANION");
        assertThat(first.path("payload").path("knowledgeType").asText()).isEqualTo("PERSONALITY");
        assertThat(first.path("payload").path("enabled").asBoolean()).isTrue();

        deletePoint(ROLE_COLLECTION, pointId);
    }

    private static void assertCollectionStructure(String collectionName) throws Exception {
        HttpResponse<String> response = send("GET", "/collections/" + collectionName, null);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);

        JsonNode vectors = OBJECT_MAPPER.readTree(response.body()).path("result").path("config").path("params").path("vectors");
        assertThat(vectors.path("size").asInt()).isEqualTo(VECTOR_SIZE);
        assertThat(vectors.path("distance").asText().toLowerCase(Locale.ROOT)).isEqualTo("cosine");
    }

    private static void upsertPoint(String collectionName, String pointId, String payload) throws Exception {
        String body = """
            {
              "points": [
                {
                  "id": %s,
                  "vector": %s,
                  "payload": %s
                }
              ]
            }
            """.formatted(pointId, vectorJson(), payload);
        HttpResponse<String> response = send("PUT", "/collections/" + collectionName + "/points?wait=true", body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private static JsonNode search(String collectionName, String filter) throws Exception {
        String body = """
            {
              "vector": %s,
              "filter": %s,
              "limit": 1,
              "with_payload": true
            }
            """.formatted(vectorJson(), filter);
        HttpResponse<String> response = send("POST", "/collections/" + collectionName + "/points/search", body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        JsonNode result = OBJECT_MAPPER.readTree(response.body());
        assertThat(result.path("result")).hasSize(1);
        return result;
    }

    private static void deletePoint(String collectionName, String pointId) throws Exception {
        String body = """
            {
              "points": [%s]
            }
            """.formatted(pointId);
        HttpResponse<String> response = send("POST", "/collections/" + collectionName + "/points/delete?wait=true", body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private static String vectorJson() {
        StringBuilder vector = new StringBuilder("[");
        for (int i = 0; i < VECTOR_SIZE; i++) {
            if (i > 0) {
                vector.append(',');
            }
            vector.append(i == 0 ? "1.0" : "0.0");
        }
        return vector.append(']').toString();
    }

    private static HttpResponse<String> send(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest
            .newBuilder(URI.create(BASE_URL + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json");
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
