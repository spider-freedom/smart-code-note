package com.smartcodenote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Chat context window management configuration.
 *
 * Controls how much conversation history is included in each AI request
 * to stay within the model's token limit.
 */
@Component
@ConfigurationProperties(prefix = "smart-code-note.chat")
public class ChatProperties {

    /** Maximum conversation rounds kept in context. 1 round = 1 user msg + 1 AI reply. */
    private int maxHistoryRounds = 10;

    /** Estimated max tokens for the full prompt (system + RAG + history + user message). */
    private int maxTotalTokens = 6000;

    public int getMaxHistoryRounds() { return maxHistoryRounds; }
    public void setMaxHistoryRounds(int maxHistoryRounds) { this.maxHistoryRounds = maxHistoryRounds; }
    public int getMaxTotalTokens() { return maxTotalTokens; }
    public void setMaxTotalTokens(int maxTotalTokens) { this.maxTotalTokens = maxTotalTokens; }
}
