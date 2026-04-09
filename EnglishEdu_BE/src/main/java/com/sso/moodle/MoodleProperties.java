package com.sso.moodle;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConfigurationProperties(prefix = "moodle")
@Getter
@Setter
public class MoodleProperties {
    private String url;
    /** Browser-facing Moodle URL (e.g. http://14.225.217.172:8080). Falls back to {@code url} if not set. */
    private String publicUrl;
    private String token;
    private String ssoSecret;
    private String serviceName = "moodle_mobile_app";

    @PostConstruct
    void validate() {
        if (token == null || token.isBlank()) {
            log.warn("MOODLE_TOKEN is not set — Moodle integration will be disabled until configured");
        }
        if (ssoSecret == null || ssoSecret.isBlank() || "change-me-strong-random-secret".equals(ssoSecret)) {
            log.warn("MOODLE_SSO_SECRET is using default value — SSO will be insecure. Set MOODLE_SSO_SECRET env var");
        }
    }

    /** Returns the public URL for browser redirects, falling back to the internal URL. */
    public String getPublicUrl() {
        return (publicUrl != null && !publicUrl.isBlank()) ? publicUrl : url;
    }
}
