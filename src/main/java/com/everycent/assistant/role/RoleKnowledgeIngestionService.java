package com.everycent.assistant.role;

import com.everycent.assistant.memory.EmbeddingClient;
import com.everycent.assistant.memory.VectorStoreClient;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RoleKnowledgeIngestionService {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStoreClient;

    public RoleKnowledgeIngestionService(EmbeddingClient embeddingClient, VectorStoreClient vectorStoreClient) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }

    public String saveRoleKnowledgeVector(Long roleProfileId, Long roleKnowledgeId, String text, Map<String, Object> payload) {
        float[] vector = embeddingClient.embed(text);
        return vectorStoreClient.upsertRoleKnowledge(roleProfileId, roleKnowledgeId, vector, payload);
    }

    public void deleteVector(String vectorId) {
        vectorStoreClient.delete(vectorId);
    }
}
