package com.itheima.smartcodenote.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${smart-code-note.ai.deepseek.api-key:}')")
public class DeepSeekAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiService.class);

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[[\\s\\S]*\\]", Pattern.MULTILINE);
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekAiService(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ── Knowledge Extraction ──────────────────────────────────────────────

    @Override
    public List<GeneratedKnowledgePoint> extractKnowledgePoints(String noteTitle, String noteContent) {
        String system = """
                你是一位资深技术导师，擅长从开发笔记中提取关键知识点。
                请从以下笔记内容中提取3-5个最重要的知识点。

                要求：
                1. 每个知识点包含 title（简洁标题）、type（类型，取值为 concept/code/configuration/troubleshooting/command）、summary（2-3句话的摘要）、difficulty（难度，取值为 easy/medium/hard）
                2. 返回纯JSON数组，不要包含markdown代码块标记
                3. 标题和摘要必须使用简体中文，要精炼准确""";

        String user = "笔记标题：" + noteTitle + "\n\n笔记内容：\n" + truncate(noteContent, 6000);

        String raw = chat(system, user);
        JsonNode array = parseJsonArray(raw);
        return convertKnowledgePoints(array);
    }

    @Override
    public void extractKnowledgePointsStream(
            String noteTitle,
            String noteContent,
            Consumer<String> onChunk,
            Consumer<List<GeneratedKnowledgePoint>> onComplete,
            Consumer<Throwable> onError) {

        String system = """
                你是一位资深技术导师，擅长从开发笔记中提取关键知识点。
                请从以下笔记内容中提取3-5个最重要的知识点。

                要求：
                1. 每个知识点包含 title（简洁标题）、type（类型，取值为 concept/code/configuration/troubleshooting/command）、summary（2-3句话的摘要）、difficulty（难度，取值为 easy/medium/hard）
                2. 返回纯JSON数组，不要包含markdown代码块标记
                3. 标题和摘要必须使用简体中文，要精炼准确""";

        String user = "笔记标题：" + noteTitle + "\n\n笔记内容：\n" + truncate(noteContent, 6000);

        chatStream(system, user, onChunk)
                .thenAccept(raw -> {
                    try {
                        JsonNode array = parseJsonArray(raw);
                        List<GeneratedKnowledgePoint> result = convertKnowledgePoints(array);
                        onComplete.accept(result);
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                })
                .exceptionally(ex -> {
                    onError.accept(ex);
                    return null;
                });
    }

    // ── Question Generation ────────────────────────────────────────────────

    @Override
    public List<GeneratedQuestion> generateQuestions(String knowledgeTitle, String knowledgeSummary, int count) {
        String system = """
                你是一位技术考试命题专家，擅长基于知识点生成高质量的练习题。
                请根据以下知识点，生成%d道不同类型的练习题。

                要求：
                1. 所有题目内容、选项、标准答案和解析必须使用简体中文
                2. 题目类型必须多样：包含单选题(single_choice)、判断题(judgement)、简答题(short_answer)
                3. 对于单选题：options数组必须包含4个选项(A/B/C/D)，每个选项有optionKey、optionContent和correct(true/false)字段，有且仅有一个正确选项，选项内容使用简体中文
                4. 对于判断题：options包含true和false两个选项，其中一个correct为true
                5. 对于简答题：options为空数组[]
                6. 每题包含 content（题目内容）、standardAnswer（标准答案）、analysis（解析）、difficulty（easy/medium/hard）
                7. 返回纯JSON数组，不要包含markdown代码块标记
                8. 题目要有区分度，不要过于简单""".formatted(count);

        String user = "知识点：" + knowledgeTitle + "\n\n知识点摘要：" + knowledgeSummary;

        String raw = chat(system, user);
        JsonNode array = parseJsonArray(raw);
        return convertQuestions(array);
    }

    @Override
    public void generateQuestionsStream(
            String knowledgeTitle,
            String knowledgeSummary,
            int count,
            Consumer<String> onChunk,
            Consumer<List<GeneratedQuestion>> onComplete,
            Consumer<Throwable> onError) {

        String system = """
                你是一位技术考试命题专家，擅长基于知识点生成高质量的练习题。
                请根据以下知识点，生成%d道不同类型的练习题。

                要求：
                1. 所有题目内容、选项、标准答案和解析必须使用简体中文
                2. 题目类型必须多样：包含单选题(single_choice)、判断题(judgement)、简答题(short_answer)
                3. 对于单选题：options数组必须包含4个选项(A/B/C/D)，每个选项有optionKey、optionContent和correct(true/false)字段，有且仅有一个正确选项，选项内容使用简体中文
                4. 对于判断题：options包含true和false两个选项，其中一个correct为true
                5. 对于简答题：options为空数组[]
                6. 每题包含 content（题目内容）、standardAnswer（标准答案）、analysis（解析）、difficulty（easy/medium/hard）
                7. 返回纯JSON数组，不要包含markdown代码块标记
                8. 题目要有区分度，不要过于简单""".formatted(count);

        String user = "知识点：" + knowledgeTitle + "\n\n知识点摘要：" + knowledgeSummary;

        chatStream(system, user, onChunk)
                .thenAccept(raw -> {
                    try {
                        JsonNode array = parseJsonArray(raw);
                        List<GeneratedQuestion> result = convertQuestions(array);
                        onComplete.accept(result);
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                })
                .exceptionally(ex -> {
                    onError.accept(ex);
                    return null;
                });
    }

    // ── Subjective Scoring ─────────────────────────────────────────────────

    @Override
    public AiScore scoreSubjectiveAnswer(String questionContent, String standardAnswer, String userAnswer) {
        String system = """
                你是一位严格公正的技术题目评分专家。
                请根据标准答案对用户的回答进行评分。

                要求：
                1. 综合评估回答的准确性、完整性和深度
                2. score为0-100的整数，60分及以上为及格
                3. comment为1-2句话的简短评语，指出优点和不足，必须使用简体中文
                4. 返回纯JSON对象：{"score": 数字, "comment": "评语"}，不要包含markdown代码块标记""";

        String user = "题目：" + questionContent
                + "\n\n标准答案：" + standardAnswer
                + "\n\n用户回答：" + userAnswer;

        String raw = chat(system, user);
        JsonNode obj = parseJsonObject(raw);
        int score = clampScore(obj.has("score") ? obj.get("score").asInt() : 0);
        String comment = obj.has("comment") ? obj.get("comment").asText() : "评分完成";
        return new AiScore(score, comment);
    }

    // ── HTTP Communication ─────────────────────────────────────────────────

    private String chat(String system, String user) {
        String body = buildRequestBody(system, user, false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String errorMsg = extractErrorMessage(response.body());
                log.error("DeepSeek API error ({}): {}", response.statusCode(), errorMsg);
                throw new RuntimeException("AI服务调用失败: " + errorMsg);
            }
            return objectMapper.readTree(response.body())
                    .get("choices").get(0)
                    .get("message").get("content").asText();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("DeepSeek API request failed", e);
            throw new RuntimeException("AI服务请求失败: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<String> chatStream(String system, String user, Consumer<String> onChunk) {
        String body = buildRequestBody(system, user, true);
        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        resultFuture.completeExceptionally(
                                new RuntimeException("AI服务调用失败"));
                        return;
                    }
                    StringBuilder contentBuilder = new StringBuilder();
                    response.body().forEach(line -> {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                return;
                            }
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                JsonNode choices = node.get("choices");
                                if (choices != null && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        String chunk = delta.get("content").asText();
                                        contentBuilder.append(chunk);
                                        onChunk.accept(chunk);
                                    }
                                }
                            } catch (JsonProcessingException ignored) {
                                // Skip unparseable SSE lines
                            }
                        }
                    });
                    resultFuture.complete(contentBuilder.toString());
                })
                .exceptionally(ex -> {
                    resultFuture.completeExceptionally(ex);
                    return null;
                });

        return resultFuture;
    }

    private String buildRequestBody(String system, String user, boolean stream) {
        try {
            var messages = List.of(
                    java.util.Map.of("role", "system", "content", system),
                    java.util.Map.of("role", "user", "content", user));
            var body = new java.util.LinkedHashMap<String, Object>();
            body.put("model", properties.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.3);
            body.put("max_tokens", 4096);
            body.put("stream", stream);
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build request body", e);
        }
    }

    // ── JSON Parsing with Error Recovery ───────────────────────────────────

    private JsonNode parseJsonArray(String raw) {
        String cleaned = extractJson(raw, true);
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            if (!node.isArray()) {
                throw new RuntimeException("Expected JSON array but got: " + node.getNodeType());
            }
            if (node.size() == 0) {
                throw new RuntimeException("AI未生成任何内容，请重试");
            }
            return node;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON array. Raw: {}", raw.substring(0, Math.min(500, raw.length())));
            throw new RuntimeException("AI返回格式异常，请重试", e);
        }
    }

    private JsonNode parseJsonObject(String raw) {
        String cleaned = extractJson(raw, false);
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            if (!node.isObject()) {
                throw new RuntimeException("Expected JSON object but got: " + node.getNodeType());
            }
            return node;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON object. Raw: {}", raw.substring(0, Math.min(500, raw.length())));
            throw new RuntimeException("AI返回格式异常，请重试", e);
        }
    }

    private String extractJson(String raw, boolean isArray) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("AI返回内容为空");
        }

        // 1. Try stripping markdown code blocks
        Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(raw);
        if (codeMatcher.find()) {
            return codeMatcher.group(1).trim();
        }

        // 2. Try finding JSON by pattern
        Pattern pattern = isArray ? JSON_ARRAY_PATTERN : JSON_OBJECT_PATTERN;
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        // 3. Return trimmed raw as last resort
        return raw.trim();
    }

    // ── Response Conversion ────────────────────────────────────────────────

    private List<GeneratedKnowledgePoint> convertKnowledgePoints(JsonNode array) {
        List<GeneratedKnowledgePoint> result = new ArrayList<>();
        for (JsonNode node : array) {
            result.add(new GeneratedKnowledgePoint(
                    node.has("title") ? node.get("title").asText() : "未命名知识点",
                    node.has("type") ? node.get("type").asText() : "concept",
                    node.has("summary") ? node.get("summary").asText() : "",
                    node.has("difficulty") ? node.get("difficulty").asText() : "medium"));
        }
        return result;
    }

    private List<GeneratedQuestion> convertQuestions(JsonNode array) {
        List<GeneratedQuestion> result = new ArrayList<>();
        for (JsonNode node : array) {
            List<GeneratedQuestionOption> options = new ArrayList<>();
            if (node.has("options") && node.get("options").isArray()) {
                for (JsonNode opt : node.get("options")) {
                    options.add(new GeneratedQuestionOption(
                            opt.has("optionKey") ? opt.get("optionKey").asText() : "A",
                            opt.has("optionContent") ? opt.get("optionContent").asText() : "",
                            opt.has("correct") && opt.get("correct").asBoolean()));
                }
            }
            result.add(new GeneratedQuestion(
                    node.has("questionType") ? node.get("questionType").asText() : "short_answer",
                    node.has("content") ? node.get("content").asText() : "",
                    node.has("standardAnswer") ? node.get("standardAnswer").asText() : "",
                    node.has("analysis") ? node.get("analysis").asText() : "",
                    node.has("difficulty") ? node.get("difficulty").asText() : "medium",
                    options));
        }
        return result;
    }

    // ── Error Message Extraction ───────────────────────────────────────────

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode errorNode = objectMapper.readTree(responseBody).get("error");
            if (errorNode != null && errorNode.has("message")) {
                return errorNode.get("message").asText();
            }
        } catch (JsonProcessingException ignored) {
        }
        return "未知错误";
    }

    // ── Chat Stream ────────────────────────────────────────────────────────

    @Override
    public void chatStream(
            String systemPrompt,
            String userPrompt,
            Consumer<String> onChunk,
            Consumer<String> onComplete,
            Consumer<Throwable> onError) {

        var messages = List.of(
                java.util.Map.of("role", "system", "content", systemPrompt),
                java.util.Map.of("role", "user", "content", userPrompt));

        try {
            var body = new java.util.LinkedHashMap<String, Object>();
            body.put("model", properties.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.8);
            body.put("max_tokens", 1024);
            body.put("stream", true);
            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/v1/chat/completions"))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            onError.accept(new RuntimeException("AI服务调用失败"));
                            return;
                        }
                        StringBuilder contentBuilder = new StringBuilder();
                        response.body().forEach(line -> {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) {
                                    return;
                                }
                                try {
                                    JsonNode node = objectMapper.readTree(data);
                                    JsonNode choices = node.get("choices");
                                    if (choices != null && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            String chunk = delta.get("content").asText();
                                            contentBuilder.append(chunk);
                                            onChunk.accept(chunk);
                                        }
                                    }
                                } catch (JsonProcessingException ignored) {
                                }
                            }
                        });
                        onComplete.accept(contentBuilder.toString());
                    })
                    .exceptionally(ex -> {
                        onError.accept(ex);
                        return null;
                    });
        } catch (JsonProcessingException e) {
            onError.accept(e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static String truncate(String content, int maxChars) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() <= maxChars ? content : content.substring(0, maxChars);
    }

    private static int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
