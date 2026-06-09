package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionDetailResponse {

    private Long id;
    private Long noteId;
    private Long knowledgeId;
    private String questionType;
    private String content;
    private String standardAnswer;
    private String analysis;
    private String difficulty;
    private List<QuestionOptionResponse> options;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
