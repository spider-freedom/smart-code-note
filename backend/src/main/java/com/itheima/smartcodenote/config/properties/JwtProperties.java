package com.itheima.smartcodenote.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-code-note.jwt")
public class JwtProperties {

    private String secret;

    private long expireMinutes;
}
