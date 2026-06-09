package com.itheima.smartcodenote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-code-note.wechat")
public class WechatProperties {

    private String appId;

    private String appSecret;
}
