package com.itheima.smartcodenote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NoteUploadResponse {

    private Long id;
    private String title;
    private String fileType;
    private Integer parseStatus;
    private Integer originalLength;
    private Integer cleanLength;
}
