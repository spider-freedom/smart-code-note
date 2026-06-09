package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WrongQuestionListItemResponse {

    private Long id;
    private Long questionId;
    private Long noteId;
    private Long knowledgeId;
    private String questionType;
    private String content;
    private String difficulty;
    private Integer wrongCount;
    private Boolean mastered;
    private LocalDateTime lastWrongTime;
    private LocalDateTime createTime;
}
