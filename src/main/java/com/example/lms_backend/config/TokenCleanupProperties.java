package com.example.lms_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.token-cleanup")
public record TokenCleanupProperties(
        String cron) {
}
