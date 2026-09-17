package com.yourco.saas.billing.dto;

import com.yourco.saas.domain.billing.Subscription;
import com.yourco.saas.domain.billing.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionResponse(
        SubscriptionStatus status,
        String stripeSubscriptionId,
        String stripePriceId,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd
) {
    public static SubscriptionResponse from(Subscription subscription) {
        if (subscription == null) {
            return null;
        }
        return new SubscriptionResponse(
                subscription.getStatus(),
                subscription.getStripeSubscriptionId(),
                subscription.getStripePriceId(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd()
        );
    }
}
