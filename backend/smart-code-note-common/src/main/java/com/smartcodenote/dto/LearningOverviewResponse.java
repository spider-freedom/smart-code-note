package com.smartcodenote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningOverviewResponse {

    private Long noteCount;
    private Long knowledgeCount;
    private Long questionCount;
    private Long answerCount;
    private Long correctAnswerCount;
    private Double correctRate;
    private Long wrongQuestionCount;
    private Long masteredWrongQuestionCount;
    private Long dueReviewCount;
    private Double averageMasteryLevel;
}
