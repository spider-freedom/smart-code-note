package com.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionListItemResponse {

    private Long id;
    private Long noteId;
    private Long knowledgeId;
    private String questionType;
    private String content;
    private String difficulty;
    private LocalDateTime createTime;
}
