package com.everycent.assistant.role;

import com.everycent.assistant.memory.EmbeddingClient;
import com.everycent.assistant.memory.VectorSearchResult;
import com.everycent.assistant.memory.VectorStoreClient;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoleKnowledgeRetrievalService {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStoreClient;

    public RoleKnowledgeRetrievalService(EmbeddingClient embeddingClient, VectorStoreClient vectorStoreClient) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }

    public List<VectorSearchResult> searchRoleKnowledge(Long roleProfileId, String queryText, int limit) {
        float[] queryVector = embeddingClient.embed(queryText);
        return vectorStoreClient.searchRoleKnowledge(roleProfileId, queryVector, limit);
    }

    public List<VectorSearchResult> searchRoleKnowledge(Long roleProfileId, float[] queryVector, int limit) {
        return vectorStoreClient.searchRoleKnowledge(roleProfileId, queryVector, limit);
    }
}
