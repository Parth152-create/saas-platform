package com.yourco.saas.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    private final EmailProperties properties;

    public LoggingEmailSender(EmailProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) throws EmailDeliveryException {
        if (!properties.isEnabled() || "noop".equalsIgnoreCase(properties.getProvider())) {
            log.debug("Email delivery disabled or set to noop. Skipping email to {}", maskEmail(message.to()));
            return;
        }

        // Safe logging without leaking sensitive tokens or credential payloads
        log.info("Email dispatched via [{}] provider: From='{} <{}>', To='{}', Subject='{}'",
                properties.getProvider(),
                properties.getFromName(),
                properties.getFromAddress(),
                maskEmail(message.to()),
                message.subject());
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled();
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
