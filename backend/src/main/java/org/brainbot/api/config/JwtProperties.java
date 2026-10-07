package org.brainbot.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brainbot.jwt")
public record JwtProperties(String secret, long expirationSeconds) { }
