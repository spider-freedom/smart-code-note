package com.smartcodenote.dto;

import com.smartcodenote.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WrongQuestionQueryRequest extends PageQuery {

    private Long questionId;
    private Integer mastered;
}
