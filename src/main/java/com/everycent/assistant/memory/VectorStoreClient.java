package com.everycent.assistant.memory;

import java.util.List;
import java.util.Map;

public interface VectorStoreClient {
    String upsertMemory(Long userId, Long memoryId, float[] vector, Map<String, Object> payload);

    String upsertRoleKnowledge(Long roleProfileId, Long roleKnowledgeId, float[] vector, Map<String, Object> payload);

    List<VectorSearchResult> searchMemory(Long userId, float[] queryVector, int limit);

    List<VectorSearchResult> searchRoleKnowledge(Long roleProfileId, float[] queryVector, int limit);

    void delete(String vectorId);
}
