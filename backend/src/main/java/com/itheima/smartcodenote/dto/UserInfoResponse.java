package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoResponse {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String avatar;

    private LocalDateTime createTime;
}
