package com.yourco.saas.common.email;

public record EmailMessage(
        String to,
        String subject,
        String textBody,
        String htmlBody
) {}
