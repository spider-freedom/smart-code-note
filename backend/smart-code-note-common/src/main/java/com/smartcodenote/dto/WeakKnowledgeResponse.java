package com.smartcodenote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeakKnowledgeResponse {

    private Long knowledgeId;
    private Long noteId;
    private String title;
    private String type;
    private String difficulty;
    private Integer masteryLevel;
    private Long answerCount;
    private Long wrongCount;
    private Double correctRate;
    private Double weaknessScore;
}
