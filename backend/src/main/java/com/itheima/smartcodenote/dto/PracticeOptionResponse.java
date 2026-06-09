package com.itheima.smartcodenote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PracticeOptionResponse {

    private Long id;
    private String optionKey;
    private String optionContent;
}
