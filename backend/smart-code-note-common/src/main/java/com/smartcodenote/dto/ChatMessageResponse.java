package com.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
