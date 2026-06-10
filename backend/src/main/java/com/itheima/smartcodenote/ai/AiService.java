package com.itheima.smartcodenote.ai;

import java.util.List;
import java.util.function.Consumer;

public interface AiService {

    List<GeneratedKnowledgePoint> extractKnowledgePoints(String noteTitle, String noteContent);

    List<GeneratedQuestion> generateQuestions(String knowledgeTitle, String knowledgeSummary, int count);

    AiScore scoreSubjectiveAnswer(String questionContent, String standardAnswer, String userAnswer);

    default void chatStream(
            String systemPrompt,
            String userPrompt,
            Consumer<String> onChunk,
            Consumer<String> onComplete,
            Consumer<Throwable> onError) {
        try {
            // Default fallback: use mock reply
            onComplete.accept("你好！我是小码，你的学习伙伴。当前 AI 服务未配置，请设置 DeepSeek API Key 后使用。");
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    default void extractKnowledgePointsStream(
            String noteTitle,
            String noteContent,
            Consumer<String> onChunk,
            Consumer<List<GeneratedKnowledgePoint>> onComplete,
            Consumer<Throwable> onError) {
        try {
            List<GeneratedKnowledgePoint> result = extractKnowledgePoints(noteTitle, noteContent);
            onComplete.accept(result);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    default void generateQuestionsStream(
            String knowledgeTitle,
            String knowledgeSummary,
            int count,
            Consumer<String> onChunk,
            Consumer<List<GeneratedQuestion>> onComplete,
            Consumer<Throwable> onError) {
        try {
            List<GeneratedQuestion> result = generateQuestions(knowledgeTitle, knowledgeSummary, count);
            onComplete.accept(result);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    record GeneratedKnowledgePoint(String title, String type, String summary, String difficulty) {
    }

    record GeneratedQuestion(
            String questionType,
            String content,
            String standardAnswer,
            String analysis,
            String difficulty,
            List<GeneratedQuestionOption> options) {
    }

    record GeneratedQuestionOption(String optionKey, String optionContent, boolean correct) {
    }

    record AiScore(int score, String comment) {
    }

    /**
     * Generate an embedding vector for a text.
     */
    default float[] embed(String text) {
        throw new UnsupportedOperationException("Embedding not supported by this AI service");
    }

    /**
     * Generate embedding vectors for multiple texts in batch.
     */
    default List<float[]> embedBatch(List<String> texts) {
        throw new UnsupportedOperationException("Batch embedding not supported by this AI service");
    }
}
