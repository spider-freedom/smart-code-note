package com.itheima.smartcodenote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParseStatusResponse {

    /** PENDING | PROCESSING | COMPLETED | FAILED */
    private String status;

    /** Number of knowledge points generated (0 while processing) */
    private Integer knowledgeCount;

    /** Number of questions generated (0 while processing) */
    private Integer questionCount;

    /** Error description when status is FAILED */
    private String errorMessage;
}
