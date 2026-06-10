package com.itheima.smartcodenote.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.smartcodenote.ai.AiProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Calls DeepSeek Embeddings API to generate vector embeddings for text.
 *
 * <p>Uses the OpenAI-compatible {@code /v1/embeddings} endpoint.
 * DeepSeek's embedding model returns 4096-dimensional vectors.
 */
@Service
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final AiProperties aiProperties;
    private final RagProperties ragProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingClient(AiProperties aiProperties, RagProperties ragProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Generate an embedding vector for a single text.
     */
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        if (results.isEmpty()) {
            throw new RuntimeException("Embedding API returned no results");
        }
        return results.get(0);
    }

    /**
     * Generate embedding vectors for multiple texts in a single API call.
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        String model = aiProperties.getEmbeddingModel();
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "input", texts));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize embedding request", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiProperties.getBaseUrl() + "/v1/embeddings"))
                .header("Authorization", "Bearer " + aiProperties.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String errorMsg = extractErrorMessage(response.body());
                log.error("Embedding API error ({}): {}", response.statusCode(), errorMsg);
                throw new RuntimeException("Embedding API call failed: " + errorMsg);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                throw new RuntimeException("Embedding API returned unexpected format");
            }

            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embeddingNode = item.get("embedding");
                if (embeddingNode == null || !embeddingNode.isArray()) continue;

                float[] vector = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    vector[i] = (float) embeddingNode.get(i).asDouble();
                }
                embeddings.add(vector);
            }

            log.debug("Generated {} embeddings (dim={}) via model {}", embeddings.size(),
                    embeddings.isEmpty() ? 0 : embeddings.get(0).length, model);
            return embeddings;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Embedding API request failed", e);
            throw new RuntimeException("Embedding API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Serialize a float[] vector to a byte array for BLOB storage.
     */
    public static byte[] serializeVector(float[] vector) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(vector.length * 4);
        buffer.asFloatBuffer().put(vector);
        return buffer.array();
    }

    /**
     * Deserialize a byte array back to a float[] vector.
     */
    public static float[] deserializeVector(byte[] bytes) {
        java.nio.FloatBuffer floatBuffer = java.nio.ByteBuffer.wrap(bytes).asFloatBuffer();
        float[] vector = new float[floatBuffer.remaining()];
        floatBuffer.get(vector);
        return vector;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode errorNode = objectMapper.readTree(responseBody).get("error");
            if (errorNode != null && errorNode.has("message")) {
                return errorNode.get("message").asText();
            }
        } catch (JsonProcessingException ignored) {
        }
        return "Unknown error";
    }
}
