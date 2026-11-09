
package com.matcher.infrastructure.adapter.ai;

import com.matcher.domain.model.ResumeChunk;
import com.matcher.domain.port.out.VectorStorePort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SpringAiVectorStoreAdapter implements VectorStorePort {

        private final VectorStore vectorStore;

        public SpringAiVectorStoreAdapter(VectorStore vectorStore) {
                this.vectorStore = vectorStore;
        }

        @Override
        public void saveChunks(List<ResumeChunk> chunks) {
                List<Document> documents = chunks.stream()
                                .map(chunk -> new Document(
                                                chunk.id().toString(),
                                                chunk.content(),
                                                chunk.metadata()))
                                .toList();

                vectorStore.add(documents);
        }

        @Override
        public List<ResumeChunk> findSimilarChunks(String queryText, int topK, UUID candidateId) {
                FilterExpressionBuilder b = new FilterExpressionBuilder();

                SearchRequest request = SearchRequest.query(queryText)
                                .withTopK(topK)
                                .withSimilarityThreshold(0.6)
                                .withFilterExpression(b.eq("candidateId", candidateId.toString()).build());

                List<Document> results = vectorStore.similaritySearch(request);

                return results.stream()
                                .map(doc -> new ResumeChunk(
                                                UUID.fromString(doc.getId()),
                                                candidateId,
                                                doc.getContent(),
                                                doc.getMetadata()))
                                .toList();
        }

        @Override
        public List<ResumeChunk> findGlobalSimilarChunks(String query, int topK) {
                SearchRequest searchRequest = SearchRequest.query(query)
                                .withTopK(topK);

                List<Document> results = vectorStore.similaritySearch(searchRequest);

                return results.stream()
                                .map(doc -> {
                                        UUID candidateId = null;
                                        if (doc.getMetadata().containsKey("candidateId")) {
                                                candidateId = UUID.fromString(
                                                                doc.getMetadata().get("candidateId").toString());
                                        }

                                        return new ResumeChunk(
                                                        UUID.fromString(doc.getId()),
                                                        candidateId,
                                                        doc.getContent(),
                                                        doc.getMetadata());
                                })
                                .toList();
        }
}
