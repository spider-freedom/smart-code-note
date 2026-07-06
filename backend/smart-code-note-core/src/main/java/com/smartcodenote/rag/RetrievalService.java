package com.smartcodenote.rag;

import com.smartcodenote.entity.NoteChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Performs vector similarity search across user's note chunks.
 *
 * <p>Uses in-memory cosine similarity — fast enough for user-scale
 * (typically 100-500 chunks per user, ~10ms).
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final RagProperties properties;

    public RetrievalService(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * Search the top-K most similar chunks to the query vector.
     *
     * @param queryVector the embedding of the user's question
     * @param chunks      all chunks belonging to the user (with embeddings)
     * @return top-K chunks above the similarity threshold, sorted by relevance
     */
    public List<ScoredChunk> search(float[] queryVector, List<NoteChunk> chunks) {
        return search(queryVector, chunks, properties.getTopK(), properties.getSimilarityThreshold());
    }

    /**
     * Search with custom parameters.
     */
    public List<ScoredChunk> search(float[] queryVector, List<NoteChunk> chunks, int topK, double threshold) {
        if (queryVector == null || chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        List<ScoredChunk> scored = new ArrayList<>();

        for (NoteChunk chunk : chunks) {
            byte[] embeddingBytes = chunk.getEmbedding();
            if (embeddingBytes == null || embeddingBytes.length == 0) continue;

            float[] chunkVector = EmbeddingClient.deserializeVector(embeddingBytes);
            if (chunkVector.length != queryVector.length) {
                log.warn("Vector dimension mismatch: query={}, chunk={}", queryVector.length, chunkVector.length);
                continue;
            }

            double similarity = cosineSimilarity(queryVector, chunkVector);
            if (similarity >= threshold) {
                scored.add(new ScoredChunk(chunk, similarity));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredChunk::similarity).reversed());

        List<ScoredChunk> result = scored.subList(0, Math.min(topK, scored.size()));
        log.debug("Retrieved {} chunks (threshold={}, topK={})", result.size(), threshold, topK);
        return result;
    }

    /**
     * Compute cosine similarity between two float vectors.
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * A chunk with its similarity score.
     */
    public record ScoredChunk(NoteChunk chunk, double similarity) {
    }
}
