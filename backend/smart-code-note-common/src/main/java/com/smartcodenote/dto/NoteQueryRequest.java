package com.smartcodenote.dto;

import com.smartcodenote.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoteQueryRequest extends PageQuery {

    private String keyword;

    private String category;
}
