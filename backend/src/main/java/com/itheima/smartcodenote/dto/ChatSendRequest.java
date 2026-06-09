package com.itheima.smartcodenote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSendRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Long sessionId;
}
