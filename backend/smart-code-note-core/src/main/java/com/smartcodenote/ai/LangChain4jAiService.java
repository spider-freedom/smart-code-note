package com.smartcodenote.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * AiService implementation powered by LangChain4j.
 *
 * Benefits over the old DeepSeekAiService:
 * - Uses LangChain4j's ChatLanguageModel / EmbeddingModel abstractions
 * - Connection pooling & retry handled by the framework
 * - Model-agnostic: switch DeepSeek → Qwen → GPT by changing config
 * - Clean separation: HTTP concerns live in AiConfig, business logic lives here
 */
@Service
@ConditionalOnProperty(name = "smart-code-note.ai.deepseek.api-key")
public class LangChain4jAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jAiService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_ARRAY = Pattern.compile("\\[[\\s\\S]*\\]", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final EmbeddingModel embeddingModel;
    private final AiProperties properties;

    public LangChain4jAiService(ChatLanguageModel chatModel,
                                EmbeddingModel embeddingModel,
                                AiProperties properties) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.properties = properties;

        // Build a separate streaming model instance for SSE
        this.streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getBaseUrl() + "/v1")
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(0.3)
                .maxTokens(4096)
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    // ── Knowledge Extraction ──────────────────────────────────────────────

    @Override
    public List<GeneratedKnowledgePoint> extractKnowledgePoints(String noteTitle, String noteContent) {
        String system = """
            你是一位资深技术导师，擅长从开发笔记中提取关键知识点。
            请从以下笔记内容中提取3-5个最重要的知识点。

            要求：
            1. 每个知识点包含 title（简洁标题）、type（concept/code/configuration/troubleshooting/command）、summary（2-3句话摘要）、difficulty（easy/medium/hard）
            2. 返回纯JSON数组，不要包含markdown代码块标记
            3. 标题和摘要必须使用简体中文""";

        String user = "笔记标题：" + noteTitle + "\n\n笔记内容：\n" + truncate(noteContent, 6000);
        String raw = chat(system, user);
        return convertKnowledgePoints(parseArray(raw));
    }

    @Override
    public void extractKnowledgePointsStream(
            String noteTitle, String noteContent,
            Consumer<String> onChunk,
            Consumer<List<GeneratedKnowledgePoint>> onComplete,
            Consumer<Throwable> onError) {
        // Streaming variant: accumulate full response, then parse
        chatStreamInternal(
                buildKnowledgeSystemPrompt(),
                "笔记标题：" + noteTitle + "\n\n笔记内容：\n" + truncate(noteContent, 6000),
                onChunk,
                raw -> {
                    try {
                        onComplete.accept(convertKnowledgePoints(parseArray(raw)));
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                },
                onError);
    }

    // ── Question Generation ────────────────────────────────────────────────

    @Override
    public List<GeneratedQuestion> generateQuestions(String knowledgeTitle, String knowledgeSummary, int count) {
        String system = """
            你是一位技术考试命题专家，请根据以下知识点，生成%d道不同类型的练习题。

            要求：
            1. 题目类型多样：single_choice（4个选项A/B/C/D）、judgement（true/false）、short_answer
            2. 返回纯JSON数组，每题包含 questionType/content/standardAnswer/analysis/difficulty/options
            3. 选项包含 optionKey/optionContent/correct 字段
            4. 题目要有区分度，所有内容使用简体中文""".formatted(count);

        String user = "知识点：" + knowledgeTitle + "\n\n知识点摘要：" + knowledgeSummary;
        String raw = chat(system, user);
        return convertQuestions(parseArray(raw));
    }

    @Override
    public void generateQuestionsStream(
            String knowledgeTitle, String knowledgeSummary, int count,
            Consumer<String> onChunk,
            Consumer<List<GeneratedQuestion>> onComplete,
            Consumer<Throwable> onError) {
        String system = buildQuestionSystemPrompt(count);
        chatStreamInternal(system,
                "知识点：" + knowledgeTitle + "\n\n知识点摘要：" + knowledgeSummary,
                onChunk,
                raw -> {
                    try {
                        onComplete.accept(convertQuestions(parseArray(raw)));
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                },
                onError);
    }

    // ── Subjective Scoring ─────────────────────────────────────────────────

    @Override
    public AiScore scoreSubjectiveAnswer(String questionContent, String standardAnswer, String userAnswer) {
        String system = """
            你是一位严格公正的技术题目评分专家。
            请根据标准答案对用户回答评分，返回纯JSON：{"score": 0-100, "comment": "1-2句评语"}
            60分及以上及格，评语必须使用简体中文。""";

        String user = "题目：" + questionContent
                + "\n\n标准答案：" + standardAnswer
                + "\n\n用户回答：" + userAnswer;

        String raw = chat(system, user);
        try {
            JsonNode obj = objectMapper.readTree(extractJson(raw, false));
            int score = Math.max(0, Math.min(100, obj.has("score") ? obj.get("score").asInt() : 0));
            String comment = obj.has("comment") ? obj.get("comment").asText() : "评分完成";
            return new AiScore(score, comment);
        } catch (Exception e) {
            log.warn("Failed to parse score: {}", e.getMessage());
            return new AiScore(60, "评分完成");
        }
    }

    // ── Chat Stream ────────────────────────────────────────────────────────

    @Override
    public void chatStream(
            String systemPrompt, String userPrompt,
            Consumer<String> onChunk,
            Consumer<String> onComplete,
            Consumer<Throwable> onError) {
        chatStreamInternal(systemPrompt, userPrompt, onChunk, onComplete, onError);
    }

    // ── Embedding ──────────────────────────────────────────────────────────

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text).content().vector();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private String chat(String system, String user) {
        ChatResponse response = chatModel.chat(
                SystemMessage.from(system),
                UserMessage.from(user));
        return response.aiMessage().text();
    }

    private void chatStreamInternal(
            String system, String user,
            Consumer<String> onChunk,
            Consumer<String> onComplete,
            Consumer<Throwable> onError) {
        StringBuilder full = new StringBuilder();
        streamingChatModel.chat(
                List.of(SystemMessage.from(system), UserMessage.from(user)),
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        full.append(partialResponse);
                        onChunk.accept(partialResponse);
                    }
                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        onComplete.accept(full.toString());
                    }
                    @Override
                    public void onError(Throwable error) {
                        onError.accept(error);
                    }
                });
    }

    // ── JSON Parsing ───────────────────────────────────────────────────────

    private JsonNode parseArray(String raw) {
        String cleaned = extractJson(raw, true);
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            if (!node.isArray()) throw new RuntimeException("Expected array");
            return node;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON array: {}", raw.substring(0, Math.min(200, raw.length())));
            throw new RuntimeException("AI返回格式异常", e);
        }
    }

    private String extractJson(String raw, boolean isArray) {
        if (raw == null || raw.isBlank()) throw new RuntimeException("AI返回内容为空");

        Matcher code = CODE_BLOCK.matcher(raw);
        if (code.find()) return code.group(1).trim();

        Pattern p = isArray ? JSON_ARRAY : Pattern.compile("\\{[\\s\\S]*\\}", Pattern.MULTILINE);
        Matcher m = p.matcher(raw);
        if (m.find()) return m.group().trim();

        return raw.trim();
    }

    // ── Converters ─────────────────────────────────────────────────────────

    private List<GeneratedKnowledgePoint> convertKnowledgePoints(JsonNode array) {
        List<GeneratedKnowledgePoint> result = new ArrayList<>();
        for (JsonNode n : array) {
            result.add(new GeneratedKnowledgePoint(
                    n.has("title") ? n.get("title").asText() : "未命名",
                    n.has("type") ? n.get("type").asText() : "concept",
                    n.has("summary") ? n.get("summary").asText() : "",
                    n.has("difficulty") ? n.get("difficulty").asText() : "medium"));
        }
        return result;
    }

    private List<GeneratedQuestion> convertQuestions(JsonNode array) {
        List<GeneratedQuestion> result = new ArrayList<>();
        for (JsonNode n : array) {
            List<GeneratedQuestionOption> opts = new ArrayList<>();
            if (n.has("options") && n.get("options").isArray()) {
                for (JsonNode o : n.get("options")) {
                    opts.add(new GeneratedQuestionOption(
                            o.has("optionKey") ? o.get("optionKey").asText() : "A",
                            o.has("optionContent") ? o.get("optionContent").asText() : "",
                            o.has("correct") && o.get("correct").asBoolean()));
                }
            }
            result.add(new GeneratedQuestion(
                    n.has("questionType") ? n.get("questionType").asText() : "short_answer",
                    n.has("content") ? n.get("content").asText() : "",
                    n.has("standardAnswer") ? n.get("standardAnswer").asText() : "",
                    n.has("analysis") ? n.get("analysis").asText() : "",
                    n.has("difficulty") ? n.get("difficulty").asText() : "medium",
                    opts));
        }
        return result;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s != null ? s : "";
    }

    private static String buildKnowledgeSystemPrompt() {
        return """
            你是一位资深技术导师，请从笔记中提取3-5个最重要的知识点。
            返回纯JSON数组：[{"title":"...","type":"concept|code|configuration|troubleshooting|command","summary":"...","difficulty":"easy|medium|hard"}]""";
    }

    private static String buildQuestionSystemPrompt(int count) {
        return """
            你是一位技术考试命题专家，请生成%d道练习题（single_choice/judgement/short_answer）。
            返回纯JSON数组：[{"questionType":"...","content":"...","standardAnswer":"...","analysis":"...","difficulty":"easy|medium|hard","options":[{"optionKey":"A","optionContent":"...","correct":true}]}]""".formatted(count);
    }
}
