package com.yourco.saas.common.email;

import com.yourco.saas.domain.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(EmailProperties.class)
public class DefaultEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmailService.class);

    private final EmailSender emailSender;
    private final EmailProperties emailProperties;

    public DefaultEmailService(EmailSender emailSender, EmailProperties emailProperties) {
        this.emailSender = emailSender;
        this.emailProperties = emailProperties;
    }

    @Override
    public void sendInvitationEmail(String recipientEmail, String tenantId, String inviteToken, Role role) {
        String subject = "You've been invited to join " + tenantId + " on Nexa";
        String textBody = String.format("""
                Hello,

                You have been invited to join the workspace '%s' with role %s on the Nexa Workforce & Operations Platform.

                To accept this invitation, please visit the portal and enter your invitation token.
                This invitation link will expire in 7 days.

                Best regards,
                The %s Team
                """, tenantId, role, emailProperties.getFromName());

        String htmlBody = String.format("""
                <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2>Welcome to Nexa</h2>
                    <p>You have been invited to join the workspace <strong>%s</strong> as a <strong>%s</strong>.</p>
                    <p>Please open the acceptance link provided by your workspace administrator to set up your account and get started.</p>
                    <p style="color: #666; font-size: 12px; margin-top: 30px;">This invitation expires in 7 days.</p>
                </div>
                """, tenantId, role);

        dispatchSafely(new EmailMessage(recipientEmail, subject, textBody, htmlBody));
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        String subject = "Nexa: Password Reset Request";
        String textBody = """
                Hello,

                A password reset request was received for your Nexa account. If you did not make this request, please ignore this email.

                Please use your secure reset link within 15 minutes to reset your password.
                """;
        String htmlBody = """
                <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2>Password Reset Request</h2>
                    <p>A request was received to reset the password for your Nexa account.</p>
                    <p>If you did not request this, please contact your administrator or ignore this email.</p>
                </div>
                """;

        dispatchSafely(new EmailMessage(recipientEmail, subject, textBody, htmlBody));
    }

    @Override
    public void sendSecurityNotification(String recipientEmail, String eventTitle, String eventDetails) {
        String subject = "Nexa Security Alert: " + eventTitle;
        String textBody = String.format("""
                Security Notification: %s

                Details: %s

                If you did not perform this activity, please contact your workspace administrator immediately.
                """, eventTitle, eventDetails);
        String htmlBody = String.format("""
                <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #dc2626;">Security Alert</h2>
                    <p><strong>%s</strong></p>
                    <p>%s</p>
                </div>
                """, eventTitle, eventDetails);

        dispatchSafely(new EmailMessage(recipientEmail, subject, textBody, htmlBody));
    }

    @Override
    public void sendAccountEvent(String recipientEmail, String subject, String message) {
        String textBody = message;
        String htmlBody = String.format("""
                <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                """, subject, message);

        dispatchSafely(new EmailMessage(recipientEmail, subject, textBody, htmlBody));
    }

    private void dispatchSafely(EmailMessage message) {
        try {
            emailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to deliver email to {}: {}",
                    LoggingEmailSender.maskEmail(message.to()), e.getMessage());
        }
    }
}
