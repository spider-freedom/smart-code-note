package com.smartcodenote.config;

import com.smartcodenote.ai.AiProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j AI configuration.
 *
 * DeepSeek API is OpenAI-compatible — OpenAiChatModel / OpenAiEmbeddingModel
 * pointed at api.deepseek.com.
 */
@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${smart-code-note.ai.deepseek.api-key:}')")
    public ChatLanguageModel chatLanguageModel(AiProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl() + "/v1")
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(0.3)
                .maxTokens(4096)
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${smart-code-note.ai.deepseek.api-key:}')")
    public EmbeddingModel embeddingModel(AiProperties properties) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(properties.getBaseUrl() + "/v1")
                .apiKey(properties.getApiKey())
                .modelName(properties.getEmbeddingModel())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
