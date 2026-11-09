

package com.matcher.domain.port.out;

import com.matcher.domain.model.ResumeChunk;
import java.util.List;
import java.util.UUID;

public interface VectorStorePort {
    
    void saveChunks(List<ResumeChunk> chunks);

    List<ResumeChunk> findSimilarChunks(String queryText, int topK, UUID candidateId);

    List<ResumeChunk> findGlobalSimilarChunks(String query, int topK);
}
