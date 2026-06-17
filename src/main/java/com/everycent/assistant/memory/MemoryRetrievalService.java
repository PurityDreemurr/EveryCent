package com.everycent.assistant.memory;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemoryRetrievalService {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStoreClient;

    public MemoryRetrievalService(EmbeddingClient embeddingClient, VectorStoreClient vectorStoreClient) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }

    public List<VectorSearchResult> searchUserMemory(Long userId, String queryText, int limit) {
        float[] queryVector = embeddingClient.embed(queryText);
        return vectorStoreClient.searchMemory(userId, queryVector, limit);
    }
}
