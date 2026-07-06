package com.smartcodenote.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateQuestionRequest {

    @NotNull(message = "knowledgeId is required")
    private Long knowledgeId;

    @Min(value = 1, message = "count must be greater than or equal to 1")
    @Max(value = 10, message = "count must be less than or equal to 10")
    private Integer count = 3;
}
