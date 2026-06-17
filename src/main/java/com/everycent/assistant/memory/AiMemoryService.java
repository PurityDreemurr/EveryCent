package com.everycent.assistant.memory;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiMemoryService {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStoreClient;

    public AiMemoryService(EmbeddingClient embeddingClient, VectorStoreClient vectorStoreClient) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }

    public String saveMemoryVector(Long userId, Long memoryId, String text, Map<String, Object> payload) {
        float[] vector = embeddingClient.embed(text);
        return vectorStoreClient.upsertMemory(userId, memoryId, vector, payload);
    }

    public void deleteVector(String vectorId) {
        vectorStoreClient.delete(vectorId);
    }
}
