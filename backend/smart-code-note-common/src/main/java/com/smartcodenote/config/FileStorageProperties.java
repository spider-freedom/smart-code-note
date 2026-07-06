package com.smartcodenote.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-code-note.file")
public class FileStorageProperties {

    private String uploadDir;

    private String maxSize;
}
