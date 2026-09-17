package com.yourco.saas.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Stripe's static com.stripe.model.billingportal.Session.create(...).
 * Exists so tests can mock it via @MockitoBean without hitting thread-local static mock issues.
 */
@Component
public class StripePortalSessionCreator {

    public Session create(SessionCreateParams params) throws StripeException {
        return Session.create(params);
    }
}
