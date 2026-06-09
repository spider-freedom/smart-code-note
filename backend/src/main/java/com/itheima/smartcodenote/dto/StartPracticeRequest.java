package com.itheima.smartcodenote.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StartPracticeRequest {

    private Long noteId;
    private Long knowledgeId;
    private String questionType;

    @Min(value = 1, message = "count must be greater than or equal to 1")
    @Max(value = 50, message = "count must be less than or equal to 50")
    private Integer count = 10;
}
