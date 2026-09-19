package com.yourco.saas.common.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private boolean enabled = true;
    private String provider = "log"; // log, noop, smtp
    private String fromAddress = "noreply@nexa.app";
    private String fromName = "Nexa Platform";
    private SmtpProperties smtp = new SmtpProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public SmtpProperties getSmtp() { return smtp; }
    public void setSmtp(SmtpProperties smtp) { this.smtp = smtp; }

    public static class SmtpProperties {
        private String host = "localhost";
        private int port = 587;
        private String username;
        private String password;
        private boolean auth = false;
        private boolean starttls = false;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isAuth() { return auth; }
        public void setAuth(boolean auth) { this.auth = auth; }
        public boolean isStarttls() { return starttls; }
        public void setStarttls(boolean starttls) { this.starttls = starttls; }
    }
}
