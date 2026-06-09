package com.itheima.smartcodenote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {

    @NotNull(message = "questionId is required")
    private Long questionId;

    @NotBlank(message = "answer is required")
    private String answer;
}
