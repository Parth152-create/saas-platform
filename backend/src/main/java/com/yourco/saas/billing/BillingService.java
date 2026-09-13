package com.yourco.saas.billing;

import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.yourco.saas.domain.billing.Customer;
import com.yourco.saas.domain.billing.CustomerRepository;
import com.yourco.saas.domain.billing.InvoiceRepository;
import com.yourco.saas.domain.billing.InvoiceStatus;
import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.billing.SubscriptionRepository;
import com.yourco.saas.domain.billing.SubscriptionStatus;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRegistryService tenantRegistryService;
    private final StripeProperties stripeProperties;

    public BillingService(CustomerRepository customerRepository,
                           SubscriptionRepository subscriptionRepository,
                           InvoiceRepository invoiceRepository,
                           TenantRegistryService tenantRegistryService,
                           StripeProperties stripeProperties) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRegistryService = tenantRegistryService;
        this.stripeProperties = stripeProperties;
    }

    @Transactional
    public void handleCheckoutSessionCompleted(Session session, TenantRecord tenant) {
        String stripeCustomerId = session.getCustomer();
        if (stripeCustomerId == null) {
            log.warn("Checkout session {} completed with no customer attached", session.getId());
            return;
        }
        findOrCreateCustomer(stripeCustomerId);
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustomerId);
        // Subscription status/price/period arrive moments later via customer.subscription.created,
        // which handleSubscriptionUpsert below fills in.
    }

    @Transactional
    public void handleSubscriptionUpsert(Subscription stripeSubscription) {
        Customer customer = findOrCreateCustomer(stripeSubscription.getCustomer());

        com.yourco.saas.domain.billing.Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .orElseGet(com.yourco.saas.domain.billing.Subscription::new);

        subscription.setCustomerId(customer.getId());
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(SubscriptionStatus.valueOf(stripeSubscription.getStatus().toUpperCase()));
        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));

        // current_period_start/end and price live on the subscription item since Stripe's
        // 2025-03-31 "Basil" API version, not on the subscription itself.
        List<SubscriptionItem> items = stripeSubscription.getItems().getData();
        if (!items.isEmpty()) {
            SubscriptionItem item = items.get(0);
            subscription.setStripePriceId(item.getPrice().getId());
            subscription.setCurrentPeriodStart(Instant.ofEpochSecond(item.getCurrentPeriodStart()));
            subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(item.getCurrentPeriodEnd()));

            String tier = stripeProperties.getPriceTiers().get(item.getPrice().getId());
            if (tier != null) {
                subscription.setPlanTier(PlanTier.valueOf(tier));
            } else {
                log.warn("No plan tier configured for Stripe price {} - check app.stripe.price-tiers", item.getPrice().getId());
            }
        }

        subscriptionRepository.save(subscription);

        if (subscription.getPlanTier() != null) {
            tenantRegistryService.findByStripeCustomerId(stripeSubscription.getCustomer())
                    .ifPresent(t -> tenantRegistryService.updatePlan(t.tenantId(), subscription.getPlanTier().name()));
        }
    }

    @Transactional
    public void handleSubscriptionDeleted(Subscription stripeSubscription) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId())
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.CANCELED);
                    subscriptionRepository.save(subscription);
                });

        tenantRegistryService.findByStripeCustomerId(stripeSubscription.getCustomer())
                .ifPresent(t -> tenantRegistryService.updatePlan(t.tenantId(), PlanTier.FREE.name()));
    }

    @Transactional
    public void handleInvoiceEvent(Invoice stripeInvoice) {
        Customer customer = findOrCreateCustomer(stripeInvoice.getCustomer());

        com.yourco.saas.domain.billing.Invoice invoice = invoiceRepository
                .findByStripeInvoiceId(stripeInvoice.getId())
                .orElseGet(com.yourco.saas.domain.billing.Invoice::new);

        invoice.setCustomerId(customer.getId());
        invoice.setStripeInvoiceId(stripeInvoice.getId());
        invoice.setStatus(InvoiceStatus.valueOf(stripeInvoice.getStatus().toUpperCase()));
        invoice.setAmountDueCents(stripeInvoice.getAmountDue());
        invoice.setAmountPaidCents(stripeInvoice.getAmountPaid() != null ? stripeInvoice.getAmountPaid() : 0L);
        invoice.setCurrency(stripeInvoice.getCurrency());
        invoice.setHostedInvoiceUrl(stripeInvoice.getHostedInvoiceUrl());
        invoice.setInvoicePdfUrl(stripeInvoice.getInvoicePdf());
        invoice.setSubscriptionId(resolveLocalSubscriptionId(stripeInvoice));

        if (stripeInvoice.getStatusTransitions() != null && stripeInvoice.getStatusTransitions().getPaidAt() != null) {
            invoice.setPaidAt(Instant.ofEpochSecond(stripeInvoice.getStatusTransitions().getPaidAt()));
        }

        invoiceRepository.save(invoice);
    }

    private Customer findOrCreateCustomer(String stripeCustomerId) {
        return customerRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setStripeCustomerId(stripeCustomerId);
                    return customerRepository.save(customer);
                });
    }

    // Invoice.getSubscription() was removed in Stripe's Basil (2025-03-31) API version;
    // the subscription reference now lives under invoice.parent.subscription_details.subscription.
    private Long resolveLocalSubscriptionId(Invoice stripeInvoice) {
        if (stripeInvoice.getParent() == null || stripeInvoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }
        String stripeSubscriptionId = stripeInvoice.getParent().getSubscriptionDetails().getSubscription();
        if (stripeSubscriptionId == null) {
            return null;
        }
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .map(com.yourco.saas.domain.billing.Subscription::getId)
                .orElse(null);
    }
}