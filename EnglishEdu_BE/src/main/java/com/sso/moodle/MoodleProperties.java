package com.sso.moodle;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
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

    /** Returns the public URL for browser redirects, falling back to the internal URL. */
    public String getPublicUrl() {
        return (publicUrl != null && !publicUrl.isBlank()) ? publicUrl : url;
    }
}
