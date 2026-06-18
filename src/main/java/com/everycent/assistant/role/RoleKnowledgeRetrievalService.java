package com.everycent.assistant.role;

import com.everycent.assistant.memory.EmbeddingClient;
import com.everycent.assistant.memory.VectorSearchResult;
import com.everycent.assistant.memory.VectorStoreClient;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoleKnowledgeRetrievalService {

    public static final int DEFAULT_ROLE_KNOWLEDGE_TOP_K = 5;

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

    public List<VectorSearchResult> searchTopRoleKnowledge(Long roleProfileId, String queryText) {
        return searchRoleKnowledge(roleProfileId, queryText, DEFAULT_ROLE_KNOWLEDGE_TOP_K);
    }

    public List<VectorSearchResult> searchRoleKnowledge(Long roleProfileId, float[] queryVector, int limit) {
        return vectorStoreClient.searchRoleKnowledge(roleProfileId, queryVector, limit);
    }

    public List<VectorSearchResult> searchTopRoleKnowledge(Long roleProfileId, float[] queryVector) {
        return searchRoleKnowledge(roleProfileId, queryVector, DEFAULT_ROLE_KNOWLEDGE_TOP_K);
    }
}
