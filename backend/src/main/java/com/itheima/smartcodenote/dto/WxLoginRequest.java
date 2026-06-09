package com.itheima.smartcodenote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginRequest {

    @NotBlank(message = "code is required")
    private String code;
}
