package com.yourco.saas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int authRequestsPerMinute = 60;
    private int apiRequestsPerMinute = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getAuthRequestsPerMinute() {
        return authRequestsPerMinute;
    }

    public void setAuthRequestsPerMinute(int authRequestsPerMinute) {
        this.authRequestsPerMinute = authRequestsPerMinute;
    }

    public int getApiRequestsPerMinute() {
        return apiRequestsPerMinute;
    }

    public void setApiRequestsPerMinute(int apiRequestsPerMinute) {
        this.apiRequestsPerMinute = apiRequestsPerMinute;
    }
}
