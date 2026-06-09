package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NoteDetailResponse {

    private Long id;
    private String title;
    private String category;
    private String tags;
    private String fileUrl;
    private String fileType;
    private String originalContent;
    private String cleanContent;
    private Integer parseStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
