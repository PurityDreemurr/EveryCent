package com.everycent.assistant.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.config.EmbeddingProperties;
import com.everycent.config.VectorStoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmbeddingVectorStoreIntegrationTest {

    private static final int EXPECTED_DIMENSION = 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldEmbedTextAndRoundTripMemoryAndRoleKnowledgeVectors() {
        OpenAiCompatibleEmbeddingClient embeddingClient = new OpenAiCompatibleEmbeddingClient(embeddingProperties(), objectMapper);
        QdrantVectorStoreClient vectorStoreClient = new QdrantVectorStoreClient(vectorStoreProperties(), objectMapper);

        float[] memoryVector = embeddingClient.embed("用户说：请记住我工作日经常因为加班点奶茶。");
        assertThat(memoryVector).hasSize(EXPECTED_DIMENSION);

        Long userId = 900001L;
        Long memoryId = System.currentTimeMillis();
        String memoryVectorId = vectorStoreClient.upsertMemory(
            userId,
            memoryId,
            memoryVector,
            Map.of(
                "conversationId",
                9L,
                "messageId",
                88L,
                "memoryType",
                "MESSAGE",
                "userEmotionTagCode",
                "STRESSED",
                "importanceScore",
                0.82,
                "createdDate",
                Instant.now().toString()
            )
        );

        List<VectorSearchResult> memoryResults = vectorStoreClient.searchMemory(userId, memoryVector, 3);
        assertThat(memoryResults).anySatisfy(result -> {
            assertThat(result.getVectorId()).isEqualTo(memoryVectorId);
            assertThat(result.getPayload()).containsEntry("userId", userId.intValue());
            assertThat(result.getPayload()).containsEntry("memoryType", "MESSAGE");
        });

        float[] roleVector = embeddingClient.embed("角色设定：你是温和克制的财务生活陪伴助手。");
        assertThat(roleVector).hasSize(EXPECTED_DIMENSION);

        Long roleProfileId = 1L;
        Long roleKnowledgeId = System.currentTimeMillis();
        String roleVectorId = vectorStoreClient.upsertRoleKnowledge(
            roleProfileId,
            roleKnowledgeId,
            roleVector,
            Map.of(
                "roleCode",
                "FINANCIAL_COMPANION",
                "knowledgeType",
                "PERSONALITY",
                "priority",
                80,
                "createdDate",
                Instant.now().toString()
            )
        );

        List<VectorSearchResult> roleResults = vectorStoreClient.searchRoleKnowledge(roleProfileId, roleVector, 3);
        assertThat(roleResults).anySatisfy(result -> {
            assertThat(result.getVectorId()).isEqualTo(roleVectorId);
            assertThat(result.getPayload()).containsEntry("roleProfileId", roleProfileId.intValue());
            assertThat(result.getPayload()).containsEntry("enabled", true);
            assertThat(result.getPayload()).containsEntry("knowledgeType", "PERSONALITY");
        });

        vectorStoreClient.delete(memoryVectorId);
        vectorStoreClient.delete(roleVectorId);
    }

    private EmbeddingProperties embeddingProperties() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setProvider(env("APP_EMBEDDING_PROVIDER", "openai-compatible"));
        properties.setBaseUrl(env("APP_EMBEDDING_BASE_URL", ""));
        properties.setApiKey(env("APP_EMBEDDING_API_KEY", ""));
        properties.setModel(env("APP_EMBEDDING_MODEL", "text-embedding-v3"));
        properties.setDimension(Integer.parseInt(env("APP_EMBEDDING_DIMENSION", Integer.toString(EXPECTED_DIMENSION))));
        properties.setTimeoutSeconds(Integer.parseInt(env("APP_EMBEDDING_TIMEOUT_SECONDS", "20")));
        return properties;
    }

    private VectorStoreProperties vectorStoreProperties() {
        VectorStoreProperties properties = new VectorStoreProperties();
        properties.setProvider(env("APP_VECTOR_STORE_PROVIDER", "qdrant"));
        properties.setBaseUrl(env("APP_VECTOR_STORE_BASE_URL", "http://127.0.0.1:6333"));
        properties.setMemoryCollection(env("APP_VECTOR_STORE_MEMORY_COLLECTION", "everycent_ai_memory"));
        properties.setRoleKnowledgeCollection(env("APP_VECTOR_STORE_ROLE_KNOWLEDGE_COLLECTION", "everycent_ai_role_knowledge"));
        properties.setTimeoutSeconds(Integer.parseInt(env("APP_VECTOR_STORE_TIMEOUT_SECONDS", "10")));
        return properties;
    }

    private String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
