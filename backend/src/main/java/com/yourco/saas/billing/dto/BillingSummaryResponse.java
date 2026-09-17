package com.yourco.saas.billing.dto;

import java.util.List;

public record BillingSummaryResponse(
        String plan,
        SubscriptionResponse subscription,
        List<InvoiceResponse> invoices
) {}
