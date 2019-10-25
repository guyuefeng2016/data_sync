package com.hdvon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "task.write")
public class WriteSyncFileTaskConfig {

    private String scheduleCorn;

    private String fileDir;

    private String syncBackFileDir;

    private String syncFileDir;

}
