package com.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionListItem {

    private Long id;
    private String title;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
