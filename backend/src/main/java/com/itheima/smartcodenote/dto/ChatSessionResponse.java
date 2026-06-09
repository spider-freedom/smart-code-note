package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionResponse {

    private Long id;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ChatMessageResponse> messages;
}
