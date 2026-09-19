package com.yourco.saas.common.email;

import com.yourco.saas.domain.user.Role;

public interface EmailService {
    void sendInvitationEmail(String recipientEmail, String tenantId, String inviteToken, Role role);
    void sendPasswordResetEmail(String recipientEmail, String resetToken);
    void sendSecurityNotification(String recipientEmail, String eventTitle, String eventDetails);
    void sendAccountEvent(String recipientEmail, String subject, String message);
}
