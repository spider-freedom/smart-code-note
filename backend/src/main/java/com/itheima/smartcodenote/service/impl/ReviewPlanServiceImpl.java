package com.itheima.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.smartcodenote.dto.ReviewResultResponse;
import com.itheima.smartcodenote.dto.ReviewTaskResponse;
import com.itheima.smartcodenote.dto.SubmitReviewResultRequest;
import com.itheima.smartcodenote.entity.KnowledgePoint;
import com.itheima.smartcodenote.entity.Question;
import com.itheima.smartcodenote.entity.ReviewRecord;
import com.itheima.smartcodenote.exception.BusinessException;
import com.itheima.smartcodenote.mapper.KnowledgePointMapper;
import com.itheima.smartcodenote.mapper.QuestionMapper;
import com.itheima.smartcodenote.mapper.ReviewRecordMapper;
import com.itheima.smartcodenote.service.ReviewPlanService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReviewPlanServiceImpl implements ReviewPlanService {

    private static final int PASS_SCORE = 60;
    private static final int MAX_MASTERY_LEVEL = 5;

    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    @Override
    public void recordAnswerResult(Long userId, Long knowledgeId, Long questionId, int score, boolean correct) {
        KnowledgePoint knowledgePoint = requireOwnedKnowledge(userId, knowledgeId);
        ReviewUpdate update = updateKnowledgeReviewPlan(knowledgePoint, correct, LocalDateTime.now());
        createReviewRecord(userId, knowledgeId, questionId, correct ? "correct" : "wrong", score, update.nextReviewTime(), update.now());
    }

    @Override
    public List<ReviewTaskResponse> today(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getUserId, userId)
                        .and(query -> query
                                .isNull(KnowledgePoint::getNextReviewTime)
                                .or()
                                .le(KnowledgePoint::getNextReviewTime, now))
                        .orderByAsc(KnowledgePoint::getNextReviewTime)
                        .orderByDesc(KnowledgePoint::getCreateTime))
                .stream()
                .map(this::toTask)
                .toList();
    }

    @Override
    public ReviewResultResponse submit(Long userId, SubmitReviewResultRequest request) {
        KnowledgePoint knowledgePoint = requireOwnedKnowledge(userId, request.getKnowledgeId());
        if (request.getQuestionId() != null) {
            requireOwnedQuestion(userId, request.getQuestionId(), knowledgePoint.getId());
        }

        int score = request.getScore() == null ? defaultScore(request.getReviewResult()) : request.getScore();
        boolean success = isSuccessful(request.getReviewResult(), score);
        ReviewUpdate update = updateKnowledgeReviewPlan(knowledgePoint, success, LocalDateTime.now());
        String reviewResult = StringUtils.hasText(request.getReviewResult())
                ? request.getReviewResult().trim()
                : (success ? "remembered" : "forgotten");
        ReviewRecord record = createReviewRecord(
                userId,
                knowledgePoint.getId(),
                request.getQuestionId(),
                reviewResult,
                score,
                update.nextReviewTime(),
                update.now());

        return ReviewResultResponse.builder()
                .recordId(record.getId())
                .knowledgeId(knowledgePoint.getId())
                .questionId(request.getQuestionId())
                .reviewResult(record.getReviewResult())
                .score(record.getScore())
                .masteryLevel(knowledgePoint.getMasteryLevel())
                .nextReviewTime(knowledgePoint.getNextReviewTime())
                .createTime(record.getCreateTime())
                .build();
    }

    private ReviewUpdate updateKnowledgeReviewPlan(KnowledgePoint knowledgePoint, boolean success, LocalDateTime now) {
        int currentLevel = knowledgePoint.getMasteryLevel() == null ? 0 : knowledgePoint.getMasteryLevel();
        int nextLevel = success
                ? Math.min(MAX_MASTERY_LEVEL, currentLevel + 1)
                : Math.max(0, currentLevel - 1);
        LocalDateTime nextReviewTime = calculateNextReviewTime(nextLevel, success, now);
        knowledgePoint.setMasteryLevel(nextLevel);
        knowledgePoint.setNextReviewTime(nextReviewTime);
        knowledgePoint.setUpdateTime(now);
        knowledgePointMapper.updateById(knowledgePoint);
        return new ReviewUpdate(nextReviewTime, now);
    }

    private LocalDateTime calculateNextReviewTime(int masteryLevel, boolean success, LocalDateTime now) {
        if (!success) {
            return now.plusDays(1);
        }
        return switch (masteryLevel) {
            case 0, 1 -> now.plusDays(1);
            case 2 -> now.plusDays(2);
            case 3 -> now.plusDays(4);
            case 4 -> now.plusDays(7);
            default -> now.plusDays(15);
        };
    }

    private ReviewRecord createReviewRecord(
            Long userId,
            Long knowledgeId,
            Long questionId,
            String reviewResult,
            int score,
            LocalDateTime nextReviewTime,
            LocalDateTime now) {
        ReviewRecord record = new ReviewRecord();
        record.setUserId(userId);
        record.setKnowledgeId(knowledgeId);
        record.setQuestionId(questionId);
        record.setReviewResult(reviewResult);
        record.setScore(score);
        record.setNextReviewTime(nextReviewTime);
        record.setCreateTime(now);
        reviewRecordMapper.insert(record);
        return record;
    }

    private KnowledgePoint requireOwnedKnowledge(Long userId, Long knowledgeId) {
        KnowledgePoint knowledgePoint = knowledgePointMapper.selectOne(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getId, knowledgeId)
                .eq(KnowledgePoint::getUserId, userId)
                .last("LIMIT 1"));
        if (knowledgePoint == null) {
            throw new BusinessException(404, "knowledge point not found");
        }
        return knowledgePoint;
    }

    private Question requireOwnedQuestion(Long userId, Long questionId, Long knowledgeId) {
        Question question = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getId, questionId)
                .eq(Question::getUserId, userId)
                .eq(Question::getKnowledgeId, knowledgeId)
                .last("LIMIT 1"));
        if (question == null) {
            throw new BusinessException(404, "question not found");
        }
        return question;
    }

    private boolean isSuccessful(String reviewResult, int score) {
        if (StringUtils.hasText(reviewResult)) {
            String normalized = reviewResult.trim().toLowerCase(Locale.ROOT);
            if ("remembered".equals(normalized) || "correct".equals(normalized) || "mastered".equals(normalized)) {
                return true;
            }
            if ("forgotten".equals(normalized) || "wrong".equals(normalized) || "failed".equals(normalized)) {
                return false;
            }
        }
        return score >= PASS_SCORE;
    }

    private int defaultScore(String reviewResult) {
        return isSuccessful(reviewResult, 0) ? 100 : 0;
    }

    private ReviewTaskResponse toTask(KnowledgePoint knowledgePoint) {
        return ReviewTaskResponse.builder()
                .knowledgeId(knowledgePoint.getId())
                .noteId(knowledgePoint.getNoteId())
                .title(knowledgePoint.getTitle())
                .type(knowledgePoint.getType())
                .summary(knowledgePoint.getSummary())
                .difficulty(knowledgePoint.getDifficulty())
                .masteryLevel(knowledgePoint.getMasteryLevel())
                .nextReviewTime(knowledgePoint.getNextReviewTime())
                .build();
    }

    private record ReviewUpdate(LocalDateTime nextReviewTime, LocalDateTime now) {
    }
}
