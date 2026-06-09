package com.itheima.smartcodenote.dto;

import com.itheima.smartcodenote.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeQueryRequest extends PageQuery {

    private Long noteId;
    private String keyword;
    private String type;
    private String difficulty;
    private Integer masteryLevel;
}
