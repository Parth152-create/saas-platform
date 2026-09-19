package com.yourco.saas.common.email;

public interface EmailSender {
    void send(EmailMessage message) throws EmailDeliveryException;
    boolean isAvailable();
}
