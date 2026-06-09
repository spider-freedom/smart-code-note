package com.itheima.smartcodenote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionOptionResponse {

    private Long id;
    private String optionKey;
    private String optionContent;
    private Boolean correct;
}
