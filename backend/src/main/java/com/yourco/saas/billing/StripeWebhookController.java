package com.yourco.saas.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.yourco.saas.domain.billing.ProcessedWebhookEvent;
import com.yourco.saas.domain.billing.ProcessedWebhookEventRepository;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final TenantRegistryService tenantRegistryService;
    private final BillingService billingService;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;

    public StripeWebhookController(StripeProperties stripeProperties,
                                    TenantRegistryService tenantRegistryService,
                                    BillingService billingService,
                                    ProcessedWebhookEventRepository processedWebhookEventRepository) {
        this.stripeProperties = stripeProperties;
        this.tenantRegistryService = tenantRegistryService;
        this.billingService = billingService;
        this.processedWebhookEventRepository = processedWebhookEventRepository;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(HttpServletRequest request,
                                                 @RequestHeader("Stripe-Signature") String sigHeader) throws IOException {
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) deserializer.getObject().orElseThrow(() -> apiVersionMismatch(event));
            Optional<TenantRecord> tenant = tenantRegistryService.findByTenantId(session.getClientReferenceId());
            if (tenant.isEmpty()) {
                log.warn("Checkout session {} referenced unknown tenant '{}'", session.getId(), session.getClientReferenceId());
                return ResponseEntity.ok("tenant not found");
            }
            TenantRecord resolvedTenant = tenant.get();
            return processInTenant(resolvedTenant, event, () -> billingService.handleCheckoutSessionCompleted(session, resolvedTenant));
        }

        String stripeCustomerId = extractCustomerId(event, deserializer);
        if (stripeCustomerId == null) {
            log.debug("Ignoring Stripe event {} ({}) - no customer id to route on", event.getId(), event.getType());
            return ResponseEntity.ok("ignored");
        }

        Optional<TenantRecord> tenant = tenantRegistryService.findByStripeCustomerId(stripeCustomerId);
        if (tenant.isEmpty()) {
            log.warn("Event {} referenced unknown Stripe customer {}", event.getId(), stripeCustomerId);
            return ResponseEntity.ok("tenant not found");
        }

        TenantRecord resolvedTenant = tenant.get();
        return processInTenant(resolvedTenant, event, () -> routeEvent(event, deserializer));
    }

    private void routeEvent(Event event, EventDataObjectDeserializer deserializer) {
        switch (event.getType()) {
            case "customer.subscription.created", "customer.subscription.updated" -> {
                Subscription sub = (Subscription) deserializer.getObject().orElseThrow(() -> apiVersionMismatch(event));
                billingService.handleSubscriptionUpsert(sub);
            }
            case "customer.subscription.deleted" -> {
                Subscription sub = (Subscription) deserializer.getObject().orElseThrow(() -> apiVersionMismatch(event));
                billingService.handleSubscriptionDeleted(sub);
            }
            case "invoice.paid", "invoice.payment_failed" -> {
                Invoice invoice = (Invoice) deserializer.getObject().orElseThrow(() -> apiVersionMismatch(event));
                billingService.handleInvoiceEvent(invoice);
            }
            default -> log.debug("Ignoring unhandled Stripe event type {}", event.getType());
        }
    }

    private String extractCustomerId(Event event, EventDataObjectDeserializer deserializer) {
        return switch (event.getType()) {
            case "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted" ->
                    ((Subscription) deserializer.getObject().orElseThrow(() -> apiVersionMismatch(event))).getCustomer();
            case "invoice.paid", "invoice.payment_failed" ->
                    ((Invoice) deserializer.getObject().orElseThrow(() -> apiVersionMismatch(event))).getCustomer();
            default -> null;
        };
    }

    private ResponseEntity<String> processInTenant(TenantRecord tenant, Event event, Runnable action) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            if (processedWebhookEventRepository.existsById(event.getId())) {
                log.info("Ignoring already-processed Stripe event {}", event.getId());
                return ResponseEntity.ok("already processed");
            }
            action.run();
            processedWebhookEventRepository.save(new ProcessedWebhookEvent(event.getId(), event.getType()));
            return ResponseEntity.ok("ok");
        } finally {
            TenantContext.clear();
        }
    }

    private RuntimeException apiVersionMismatch(Event event) {
        return new IllegalStateException("Could not deserialize Stripe event " + event.getId() + " (" + event.getType()
                + ") - possible API version mismatch between the webhook endpoint and stripe-java");
    }
}