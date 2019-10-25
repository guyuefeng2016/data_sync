package com.hdvon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "task.read")
public class ReadSyncFile2DBConfig {

    private String scheduleCorn;
    private String remoteSyncFileDir;
    private String remoteBackFileDir;
    private String remoteFailBackFileDir;
}
