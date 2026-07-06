package com.smartcodenote.rag;

import dev.langchain4j.model.embedding.EmbeddingModel;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around LangChain4j's EmbeddingModel for backward compatibility.
 *
 * Previously this class made raw HttpClient calls to DeepSeek /v1/embeddings.
 * Now it delegates to LangChain4j, gaining connection pooling, retry, and
 * model-agnostic switching (DeepSeek → Qwen → OpenAI) for free.
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final EmbeddingModel embeddingModel;

    public EmbeddingClient(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text).content().vector();
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        return texts.stream().map(this::embed).toList();
    }

    public static byte[] serializeVector(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
        buffer.asFloatBuffer().put(vector);
        return buffer.array();
    }

    public static float[] deserializeVector(byte[] bytes) {
        FloatBuffer floatBuffer = ByteBuffer.wrap(bytes).asFloatBuffer();
        float[] vector = new float[floatBuffer.remaining()];
        floatBuffer.get(vector);
        return vector;
    }
}
