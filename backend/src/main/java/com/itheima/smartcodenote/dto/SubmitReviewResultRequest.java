package com.itheima.smartcodenote.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewResultRequest {

    @NotNull(message = "knowledgeId is required")
    private Long knowledgeId;

    private Long questionId;

    @Min(value = 0, message = "score must be greater than or equal to 0")
    @Max(value = 100, message = "score must be less than or equal to 100")
    private Integer score;

    private String reviewResult;
}
