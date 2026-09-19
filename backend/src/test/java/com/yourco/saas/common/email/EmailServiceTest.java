package com.yourco.saas.common.email;

import com.yourco.saas.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    private EmailProperties properties;
    private MockEmailSender mockSender;
    private DefaultEmailService emailService;

    @BeforeEach
    void setUp() {
        properties = new EmailProperties();
        properties.setEnabled(true);
        properties.setProvider("log");
        properties.setFromName("Nexa Platform");
        properties.setFromAddress("noreply@nexa.app");

        mockSender = new MockEmailSender();
        emailService = new DefaultEmailService(mockSender, properties);
    }

    @Test
    void emailMaskingProtectsPersonalDataInLogs() {
        assertEquals("a***e@example.com", LoggingEmailSender.maskEmail("alice@example.com"));
        assertEquals("b***b@workspace.io", LoggingEmailSender.maskEmail("bob@workspace.io"));
        assertEquals("j***@test.org", LoggingEmailSender.maskEmail("jo@test.org"));
        assertEquals("***", LoggingEmailSender.maskEmail("invalid-email"));
        assertEquals("***", LoggingEmailSender.maskEmail(null));
    }

    @Test
    void sendInvitationEmailFormatsSubjectAndBody() {
        emailService.sendInvitationEmail("newhire@company.test", "acme-corp", "invite-token-abc", Role.MANAGER);

        assertEquals(1, mockSender.sentMessages.size());
        EmailMessage msg = mockSender.sentMessages.get(0);
        assertEquals("newhire@company.test", msg.to());
        assertTrue(msg.subject().contains("acme-corp"));
        assertTrue(msg.textBody().contains("acme-corp"));
        assertTrue(msg.textBody().contains("MANAGER"));
        assertTrue(msg.htmlBody().contains("acme-corp"));
    }

    @Test
    void sendPasswordResetEmailFormatsSubject() {
        emailService.sendPasswordResetEmail("user@company.test", "reset-token-123");

        assertEquals(1, mockSender.sentMessages.size());
        EmailMessage msg = mockSender.sentMessages.get(0);
        assertEquals("user@company.test", msg.to());
        assertTrue(msg.subject().contains("Password Reset"));
    }

    @Test
    void sendSecurityNotificationFormatsSubjectAndDetails() {
        emailService.sendSecurityNotification("admin@company.test", "Unusual Login", "IP: 192.168.1.1");

        assertEquals(1, mockSender.sentMessages.size());
        EmailMessage msg = mockSender.sentMessages.get(0);
        assertEquals("admin@company.test", msg.to());
        assertTrue(msg.subject().contains("Unusual Login"));
        assertTrue(msg.textBody().contains("192.168.1.1"));
    }

    @Test
    void gracefulFailureHandlingWhenSenderThrowsException() {
        mockSender.shouldFail = true;

        // Does not throw an uncaught exception to caller
        assertDoesNotThrow(() ->
                emailService.sendInvitationEmail("fail@test.com", "t1", "tok", Role.USER));
    }

    private static class MockEmailSender implements EmailSender {
        final List<EmailMessage> sentMessages = new ArrayList<>();
        boolean shouldFail = false;

        @Override
        public void send(EmailMessage message) throws EmailDeliveryException {
            if (shouldFail) {
                throw new EmailDeliveryException("SMTP connection refused");
            }
            sentMessages.add(message);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}
