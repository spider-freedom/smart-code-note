package com.itheima.smartcodenote.dto;

import com.itheima.smartcodenote.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoteQueryRequest extends PageQuery {

    private String keyword;

    private String category;
}
