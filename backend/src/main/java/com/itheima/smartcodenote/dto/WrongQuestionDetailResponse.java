package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WrongQuestionDetailResponse {

    private Long id;
    private Long questionId;
    private Integer wrongCount;
    private Boolean mastered;
    private LocalDateTime lastWrongTime;
    private PracticeQuestionResponse question;
}
