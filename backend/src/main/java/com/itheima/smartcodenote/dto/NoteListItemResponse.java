package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NoteListItemResponse {

    private Long id;
    private String title;
    private String category;
    private String tags;
    private String fileType;
    private Integer parseStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
