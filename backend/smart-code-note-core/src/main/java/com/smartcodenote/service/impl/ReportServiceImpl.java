package com.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcodenote.dto.LearningOverviewResponse;
import com.smartcodenote.dto.LearningSuggestionResponse;
import com.smartcodenote.dto.WeakKnowledgeResponse;
import com.smartcodenote.entity.AnswerRecord;
import com.smartcodenote.entity.KnowledgePoint;
import com.smartcodenote.entity.Note;
import com.smartcodenote.entity.Question;
import com.smartcodenote.entity.WrongQuestion;
import com.smartcodenote.mapper.AnswerRecordMapper;
import com.smartcodenote.mapper.KnowledgePointMapper;
import com.smartcodenote.mapper.NoteMapper;
import com.smartcodenote.mapper.QuestionMapper;
import com.smartcodenote.mapper.WrongQuestionMapper;
import com.smartcodenote.service.ReportService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final NoteMapper noteMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;

    @Override
    @Cacheable(value = "learning-overview", key = "#userId")
    public LearningOverviewResponse overview(Long userId) {
        List<KnowledgePoint> knowledgePoints = selectKnowledgePoints(userId);
        List<AnswerRecord> answers = selectAnswers(userId);
        long correctAnswerCount = answers.stream()
                .filter(answer -> answer.getCorrect() != null && answer.getCorrect() == 1)
                .count();
        long masteredWrongQuestionCount = wrongQuestionMapper.selectCount(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getMastered, 1));
        long dueReviewCount = knowledgePoints.stream()
                .filter(point -> point.getNextReviewTime() == null
                        || !point.getNextReviewTime().isAfter(LocalDateTime.now()))
                .count();
        double averageMasteryLevel = knowledgePoints.stream()
                .map(KnowledgePoint::getMasteryLevel)
                .filter(level -> level != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        return LearningOverviewResponse.builder()
                .noteCount(noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId)))
                .knowledgeCount((long) knowledgePoints.size())
                .questionCount(questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getUserId, userId)))
                .answerCount((long) answers.size())
                .correctAnswerCount(correctAnswerCount)
                .correctRate(rate(correctAnswerCount, answers.size()))
                .wrongQuestionCount(wrongQuestionMapper.selectCount(new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)))
                .masteredWrongQuestionCount(masteredWrongQuestionCount)
                .dueReviewCount(dueReviewCount)
                .averageMasteryLevel(round(averageMasteryLevel))
                .build();
    }

    @Override
    public List<WeakKnowledgeResponse> weakKnowledge(Long userId, int limit) {
        Map<Long, Question> questionMap = selectQuestions(userId).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        KnowledgeStats stats = buildStats(userId, questionMap);

        return selectKnowledgePoints(userId).stream()
                .map(point -> toWeakKnowledge(point, stats))
                .sorted(Comparator.comparing(WeakKnowledgeResponse::getWeaknessScore).reversed()
                        .thenComparing(WeakKnowledgeResponse::getMasteryLevel))
                .limit(limit)
                .toList();
    }

    @Override
    public LearningSuggestionResponse suggestions(Long userId) {
        LearningOverviewResponse overview = overview(userId);
        List<WeakKnowledgeResponse> weakKnowledgePoints = weakKnowledge(userId, 3);
        List<String> suggestions = new ArrayList<>();
        if (overview.getDueReviewCount() > 0) {
            suggestions.add("今日有复习任务待完成，建议先完成复习再学习新内容。");
        }
        if (overview.getCorrectRate() < 0.7 && overview.getAnswerCount() > 0) {
            suggestions.add("正确率偏低，建议重做近期错题并对照标准答案进行分析。");
        }
        if (!weakKnowledgePoints.isEmpty()) {
            suggestions.add("优先巩固最薄弱的知识点：「" + weakKnowledgePoints.get(0).getTitle() + "」。");
        }
        if (overview.getKnowledgeCount() > 0 && overview.getQuestionCount() == 0) {
            suggestions.add("已有知识点但尚未生成练习题，建议为知识点生成题目以检验掌握程度。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("学习状态不错，保持当前节奏，定期复习已掌握的知识点。");
        }

        String summary = "你共有 "
                + overview.getKnowledgeCount()
                + " 个知识点，"
                + overview.getDueReviewCount()
                + " 个待复习任务，答题正确率 "
                + Math.round(overview.getCorrectRate() * 100)
                + "%。";
        return LearningSuggestionResponse.builder()
                .summary(summary)
                .suggestions(suggestions)
                .weakKnowledgePoints(weakKnowledgePoints)
                .build();
    }

    private WeakKnowledgeResponse toWeakKnowledge(KnowledgePoint point, KnowledgeStats stats) {
        long answerCount = stats.answerCountByKnowledgeId().getOrDefault(point.getId(), 0L);
        long correctCount = stats.correctCountByKnowledgeId().getOrDefault(point.getId(), 0L);
        long wrongCount = stats.wrongCountByKnowledgeId().getOrDefault(point.getId(), 0L);
        double correctRate = rate(correctCount, answerCount);
        int masteryLevel = point.getMasteryLevel() == null ? 0 : point.getMasteryLevel();
        double weaknessScore = (5 - masteryLevel) * 20D + wrongCount * 10D + (1 - correctRate) * 30D;

        return WeakKnowledgeResponse.builder()
                .knowledgeId(point.getId())
                .noteId(point.getNoteId())
                .title(point.getTitle())
                .type(point.getType())
                .difficulty(point.getDifficulty())
                .masteryLevel(masteryLevel)
                .answerCount(answerCount)
                .wrongCount(wrongCount)
                .correctRate(correctRate)
                .weaknessScore(round(weaknessScore))
                .build();
    }

    private KnowledgeStats buildStats(Long userId, Map<Long, Question> questionMap) {
        Map<Long, Long> answerCountByKnowledgeId = new HashMap<>();
        Map<Long, Long> correctCountByKnowledgeId = new HashMap<>();
        for (AnswerRecord answer : selectAnswers(userId)) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            Long knowledgeId = question.getKnowledgeId();
            answerCountByKnowledgeId.merge(knowledgeId, 1L, Long::sum);
            if (answer.getCorrect() != null && answer.getCorrect() == 1) {
                correctCountByKnowledgeId.merge(knowledgeId, 1L, Long::sum);
            }
        }

        Map<Long, Long> wrongCountByKnowledgeId = new HashMap<>();
        for (WrongQuestion wrongQuestion : wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId))) {
            Question question = questionMap.get(wrongQuestion.getQuestionId());
            if (question != null) {
                wrongCountByKnowledgeId.merge(
                        question.getKnowledgeId(),
                        wrongQuestion.getWrongCount() == null ? 1L : wrongQuestion.getWrongCount().longValue(),
                        Long::sum);
            }
        }
        return new KnowledgeStats(answerCountByKnowledgeId, correctCountByKnowledgeId, wrongCountByKnowledgeId);
    }

    private List<KnowledgePoint> selectKnowledgePoints(Long userId) {
        return knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getUserId, userId));
    }

    private List<Question> selectQuestions(Long userId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>().eq(Question::getUserId, userId));
    }

    private List<AnswerRecord> selectAnswers(Long userId) {
        return answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getUserId, userId));
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return round((double) numerator / denominator);
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record KnowledgeStats(
            Map<Long, Long> answerCountByKnowledgeId,
            Map<Long, Long> correctCountByKnowledgeId,
            Map<Long, Long> wrongCountByKnowledgeId) {
    }
}
