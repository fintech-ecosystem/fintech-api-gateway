package kuan.fintech.fintech_api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record GatewayJwtProperties(String issuer, String secret) {
}
