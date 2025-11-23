package com.panorama.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.jwt")
@Data
public class JwtProperties {
    /**
     * Base64 encoded secret.
     */
    private String secret = "change-me-please-change-me-please";
    /**
     * Expiration in seconds.
     */
    private long expiration = 3600;
}
