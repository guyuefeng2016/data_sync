package com.hdvon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hdvon.canal")
public class CanalConfig {

    private String canalHostIp;

    private Integer canalHostPort;

    private String ZkServers;

    private boolean isCluster;

    private String destination;

    private String username;

    private String password;

    private String syncDatabase;

    private String[] syncTables;
}
