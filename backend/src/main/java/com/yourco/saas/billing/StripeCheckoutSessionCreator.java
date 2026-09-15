package com.yourco.saas.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Stripe's static Session.create(...). Exists so tests can swap it via
 * @MockitoBean - Mockito's mockStatic() is thread-local and doesn't reach the servlet
 * container's own worker thread that TestRestTemplate calls run on, so a real bean seam is
 * needed here instead of static mocking.
 */
@Component
public class StripeCheckoutSessionCreator {

    public Session create(SessionCreateParams params) throws StripeException {
        return Session.create(params);
    }
}