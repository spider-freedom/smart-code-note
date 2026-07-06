package com.smartcodenote.dto;

import com.smartcodenote.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionQueryRequest extends PageQuery {

    private Long noteId;
    private Long knowledgeId;
    private String questionType;
    private String difficulty;
    private String keyword;
}
