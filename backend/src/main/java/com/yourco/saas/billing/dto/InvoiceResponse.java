package com.yourco.saas.billing.dto;

import com.yourco.saas.domain.billing.Invoice;
import com.yourco.saas.domain.billing.InvoiceStatus;

import java.time.Instant;

public record InvoiceResponse(
        String stripeInvoiceId,
        InvoiceStatus status,
        Long amountDueCents,
        long amountPaidCents,
        String currency,
        String hostedInvoiceUrl,
        String invoicePdfUrl,
        Instant paidAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        return new InvoiceResponse(
                invoice.getStripeInvoiceId(),
                invoice.getStatus(),
                invoice.getAmountDueCents(),
                invoice.getAmountPaidCents(),
                invoice.getCurrency(),
                invoice.getHostedInvoiceUrl(),
                invoice.getInvoicePdfUrl(),
                invoice.getPaidAt()
        );
    }
}
