package com.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerResultResponse {

    private Long recordId;
    private Long questionId;
    private String questionType;
    private String userAnswer;
    private String standardAnswer;
    private Integer score;
    private Boolean correct;
    private String aiComment;
    private LocalDateTime createTime;
}
