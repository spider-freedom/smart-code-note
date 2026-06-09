package com.itheima.smartcodenote.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnMissingBean(DeepSeekAiService.class)
public class MockAiService implements AiService {

    private static final int MAX_POINTS = 5;

    @Override
    public List<GeneratedKnowledgePoint> extractKnowledgePoints(String noteTitle, String noteContent) {
        Set<String> titles = new LinkedHashSet<>();
        for (String line : noteContent.split("\\n")) {
            String normalized = normalizeLine(line);
            if (normalized.startsWith("#")) {
                titles.add(stripMarkdownHeading(normalized));
            } else if (looksImportant(normalized)) {
                titles.add(toShortTitle(normalized));
            }
            if (titles.size() >= MAX_POINTS) {
                break;
            }
        }
        if (titles.isEmpty() && StringUtils.hasText(noteTitle)) {
            titles.add(noteTitle.trim());
        }

        List<GeneratedKnowledgePoint> result = new ArrayList<>();
        for (String title : titles) {
            String safeTitle = StringUtils.hasText(title) ? title : noteTitle;
            result.add(new GeneratedKnowledgePoint(
                    safeTitle,
                    guessType(safeTitle),
                    buildSummary(safeTitle, noteContent),
                    "medium"));
        }
        return result;
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(String knowledgeTitle, String knowledgeSummary, int count) {
        String safeTitle = StringUtils.hasText(knowledgeTitle) ? knowledgeTitle.trim() : "knowledge point";
        String safeSummary = StringUtils.hasText(knowledgeSummary) ? knowledgeSummary.trim() : safeTitle;
        List<GeneratedQuestion> questions = new ArrayList<>();
        questions.add(buildSingleChoiceQuestion(safeTitle, safeSummary));
        questions.add(buildShortAnswerQuestion(safeTitle, safeSummary));
        questions.add(buildJudgementQuestion(safeTitle, safeSummary));
        return questions.stream().limit(Math.max(1, count)).toList();
    }

    @Override
    public AiScore scoreSubjectiveAnswer(String questionContent, String standardAnswer, String userAnswer) {
        if (!StringUtils.hasText(userAnswer)) {
            return new AiScore(0, "No answer was submitted.");
        }
        String normalizedAnswer = userAnswer.toLowerCase();
        String normalizedStandard = StringUtils.hasText(standardAnswer) ? standardAnswer.toLowerCase() : "";
        int score = 60;
        for (String token : normalizedStandard.split("\\W+")) {
            if (token.length() >= 4 && normalizedAnswer.contains(token)) {
                score += 10;
            }
        }
        score = Math.min(100, score);
        String comment = score >= 80
                ? "The answer covers the key points well."
                : "The answer is partially correct and should include more of the standard answer.";
        return new AiScore(score, comment);
    }

    private GeneratedQuestion buildSingleChoiceQuestion(String title, String summary) {
        return new GeneratedQuestion(
                "single_choice",
                "Which option best describes " + title + "?",
                "A",
                "The correct option matches the generated summary of this knowledge point.",
                "medium",
                List.of(
                        new GeneratedQuestionOption("A", summary, true),
                        new GeneratedQuestionOption("B", "It is unrelated to the current note.", false),
                        new GeneratedQuestionOption("C", "It only describes file upload behavior.", false),
                        new GeneratedQuestionOption("D", "It is mainly about user authentication.", false)));
    }

    private GeneratedQuestion buildShortAnswerQuestion(String title, String summary) {
        return new GeneratedQuestion(
                "short_answer",
                "Briefly explain the key idea of " + title + ".",
                summary,
                "A good answer should cover the main meaning and usage scenario of the knowledge point.",
                "medium",
                List.of());
    }

    private GeneratedQuestion buildJudgementQuestion(String title, String summary) {
        return new GeneratedQuestion(
                "judgement",
                title + " can be reviewed by connecting the concept with its note context. True or false?",
                "true",
                "The statement is true because the generated question is based on the note summary: " + summary,
                "easy",
                List.of(
                        new GeneratedQuestionOption("true", "True", true),
                        new GeneratedQuestionOption("false", "False", false)));
    }

    private String normalizeLine(String line) {
        return line == null ? "" : line.trim();
    }

    private String stripMarkdownHeading(String line) {
        return line.replaceFirst("^#+\\s*", "").trim();
    }

    private boolean looksImportant(String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        return line.contains("：")
                || line.contains(":")
                || line.toLowerCase().contains("spring")
                || line.toLowerCase().contains("controller")
                || line.toLowerCase().contains("service")
                || line.toLowerCase().contains("sql")
                || line.toLowerCase().contains("redis")
                || line.toLowerCase().contains("linux");
    }

    private String toShortTitle(String line) {
        String compact = line.replaceAll("[`*_>#-]", "").trim();
        int colon = Math.max(compact.indexOf(':'), compact.indexOf('：'));
        if (colon > 0) {
            compact = compact.substring(0, colon);
        }
        return compact.length() > 48 ? compact.substring(0, 48) : compact;
    }

    private String guessType(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("config") || lower.contains("yaml") || lower.contains("properties")) {
            return "configuration";
        }
        if (lower.contains("controller") || lower.contains("service") || lower.contains("code")) {
            return "code";
        }
        if (lower.contains("error") || lower.contains("exception")) {
            return "troubleshooting";
        }
        if (lower.contains("sql") || lower.contains("command")) {
            return "command";
        }
        return "concept";
    }

    private String buildSummary(String title, String noteContent) {
        String compact = noteContent.replaceAll("\\s+", " ").trim();
        if (compact.length() > 120) {
            compact = compact.substring(0, 120);
        }
        return "Review point extracted from note: " + title + ". Context: " + compact;
    }
}
