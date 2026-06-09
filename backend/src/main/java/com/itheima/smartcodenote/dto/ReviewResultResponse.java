package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResultResponse {

    private Long recordId;
    private Long knowledgeId;
    private Long questionId;
    private String reviewResult;
    private Integer score;
    private Integer masteryLevel;
    private LocalDateTime nextReviewTime;
    private LocalDateTime createTime;
}
