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
    private String token;
    private String ssoSecret;
    private String serviceName = "moodle_mobile_app";
}
