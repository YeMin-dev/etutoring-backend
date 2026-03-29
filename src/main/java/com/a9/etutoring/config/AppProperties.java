package com.a9.etutoring.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** IANA zone for local parsing/formatting (assignments, allocation preview fallback, JSON Instants, meeting emails). */
    private ZoneId defaultTimeZone = ZoneId.of("Asia/Yangon");

    public ZoneId getDefaultTimeZone() {
        return defaultTimeZone;
    }

    public void setDefaultTimeZone(ZoneId defaultTimeZone) {
        this.defaultTimeZone = defaultTimeZone;
    }
}
