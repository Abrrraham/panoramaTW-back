package com.panorama.backend.model.resource;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-25 11:10:40
 * @version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource.default")
public class DefaultDataSource {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
}
