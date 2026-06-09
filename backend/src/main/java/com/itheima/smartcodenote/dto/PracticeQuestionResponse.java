package com.itheima.smartcodenote.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PracticeQuestionResponse {

    private Long id;
    private Long noteId;
    private Long knowledgeId;
    private String questionType;
    private String content;
    private String difficulty;
    private List<PracticeOptionResponse> options;
}
