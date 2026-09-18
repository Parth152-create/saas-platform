package com.yourco.saas.billing;

import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.exception.StripeException;
import com.stripe.param.billingportal.SessionCreateParams;
import com.yourco.saas.billing.dto.BillingSummaryResponse;
import com.yourco.saas.billing.dto.CreatePortalSessionResponse;
import com.yourco.saas.billing.dto.InvoiceResponse;
import com.yourco.saas.billing.dto.SubscriptionResponse;
import com.yourco.saas.domain.billing.Customer;
import com.yourco.saas.domain.billing.CustomerRepository;
import com.yourco.saas.domain.billing.InvoiceRepository;
import com.yourco.saas.domain.billing.InvoiceStatus;
import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.billing.SubscriptionRepository;
import com.yourco.saas.domain.billing.SubscriptionStatus;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRegistryService tenantRegistryService;
    private final StripeProperties stripeProperties;
    private final StripePortalSessionCreator portalSessionCreator;

    public BillingService(CustomerRepository customerRepository,
                           SubscriptionRepository subscriptionRepository,
                           InvoiceRepository invoiceRepository,
                           TenantRegistryService tenantRegistryService,
                           StripeProperties stripeProperties,
                           StripePortalSessionCreator portalSessionCreator) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRegistryService = tenantRegistryService;
        this.stripeProperties = stripeProperties;
        this.portalSessionCreator = portalSessionCreator;
    }

    public CreatePortalSessionResponse createPortalSession() {
        TenantRecord tenant = tenantRegistryService.findBySchemaName(TenantContext.getTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant context not resolved"));

        if (tenant.stripeCustomerId() == null || tenant.stripeCustomerId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Billing has not been initialized: tenant has no Stripe customer");
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(tenant.stripeCustomerId())
                .setReturnUrl(stripeProperties.getPortalReturnUrl())
                .build();

        try {
            com.stripe.model.billingportal.Session session = portalSessionCreator.create(params);
            return new CreatePortalSessionResponse(session.getUrl());
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Stripe portal session creation failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public BillingSummaryResponse getBillingSummary() {
        TenantRecord tenant = tenantRegistryService.findBySchemaName(TenantContext.getTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant context not resolved"));

        if (tenant.stripeCustomerId() == null || tenant.stripeCustomerId().isBlank()) {
            String plan = tenant.plan() != null ? tenant.plan() : PlanTier.FREE.name();
            return new BillingSummaryResponse(plan, null, List.of());
        }

        Optional<Customer> customerOpt = customerRepository.findByStripeCustomerId(tenant.stripeCustomerId());
        if (customerOpt.isEmpty()) {
            String plan = tenant.plan() != null ? tenant.plan() : PlanTier.FREE.name();
            return new BillingSummaryResponse(plan, null, List.of());
        }

        Customer customer = customerOpt.get();
        List<com.yourco.saas.domain.billing.Subscription> subscriptions =
                subscriptionRepository.findByCustomerIdOrderByUpdatedAtDesc(customer.getId());
        Optional<com.yourco.saas.domain.billing.Subscription> subscriptionOpt =
                findCurrentSubscription(subscriptions);
        List<com.yourco.saas.domain.billing.Invoice> invoices =
                invoiceRepository.findByCustomerId(customer.getId());

        SubscriptionResponse subscriptionResponse = subscriptionOpt
                .map(SubscriptionResponse::from)
                .orElse(null);

        String plan = resolveEffectivePlan(subscriptionOpt, tenant);

        List<InvoiceResponse> invoiceResponses = invoices.stream()
                .map(InvoiceResponse::from)
                .toList();

        return new BillingSummaryResponse(plan, subscriptionResponse, invoiceResponses);
    }

    public static boolean isEntitledStatus(SubscriptionStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case ACTIVE, TRIALING, PAST_DUE -> true;
            case PAUSED, UNPAID, INCOMPLETE, INCOMPLETE_EXPIRED, CANCELED -> false;
        };
    }

    public static int compareRecency(com.yourco.saas.domain.billing.Subscription a,
                                     com.yourco.saas.domain.billing.Subscription b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        if (a.getCurrentPeriodStart() != null && b.getCurrentPeriodStart() != null) {
            int cmp = a.getCurrentPeriodStart().compareTo(b.getCurrentPeriodStart());
            if (cmp != 0) return cmp;
        } else if (a.getCurrentPeriodStart() != null) {
            return 1;
        } else if (b.getCurrentPeriodStart() != null) {
            return -1;
        }

        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
            int cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
            if (cmp != 0) return cmp;
        } else if (a.getCreatedAt() != null) {
            return 1;
        } else if (b.getCreatedAt() != null) {
            return -1;
        }

        if (a.getUpdatedAt() != null && b.getUpdatedAt() != null) {
            int cmp = a.getUpdatedAt().compareTo(b.getUpdatedAt());
            if (cmp != 0) return cmp;
        } else if (a.getUpdatedAt() != null) {
            return 1;
        } else if (b.getUpdatedAt() != null) {
            return -1;
        }

        if (a.getId() != null && b.getId() != null) {
            return a.getId().compareTo(b.getId());
        } else if (a.getId() != null) {
            return 1;
        } else if (b.getId() != null) {
            return -1;
        }

        return 0;
    }

    public static Optional<com.yourco.saas.domain.billing.Subscription> findCurrentSubscription(
            List<com.yourco.saas.domain.billing.Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return Optional.empty();
        }

        List<com.yourco.saas.domain.billing.Subscription> entitled = subscriptions.stream()
                .filter(s -> isEntitledStatus(s.getStatus()))
                .toList();

        if (!entitled.isEmpty()) {
            return entitled.stream().max(BillingService::compareRecency);
        }

        return subscriptions.stream().max(BillingService::compareRecency);
    }

    public Optional<com.yourco.saas.domain.billing.Subscription> findCurrentSubscription(Long customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        List<com.yourco.saas.domain.billing.Subscription> subscriptions =
                subscriptionRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId);
        return findCurrentSubscription(subscriptions);
    }

    public boolean hasActiveSubscription(TenantRecord tenant) {
        if (tenant == null || tenant.stripeCustomerId() == null || tenant.stripeCustomerId().isBlank()) {
            return false;
        }
        return customerRepository.findByStripeCustomerId(tenant.stripeCustomerId())
                .flatMap(customer -> findCurrentSubscription(customer.getId()))
                .map(sub -> isEntitledStatus(sub.getStatus()))
                .orElse(false);
    }

    public PlanTier resolveEffectivePlanTier(
            Optional<com.yourco.saas.domain.billing.Subscription> currentSubscription,
            TenantRecord tenant) {
        if (currentSubscription != null && currentSubscription.isPresent()) {
            com.yourco.saas.domain.billing.Subscription sub = currentSubscription.get();
            if (isEntitledStatus(sub.getStatus()) && sub.getPlanTier() != null) {
                return sub.getPlanTier();
            }
            return PlanTier.FREE;
        }

        if (tenant != null && tenant.plan() != null) {
            try {
                return PlanTier.valueOf(tenant.plan().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return PlanTier.FREE;
    }

    public String resolveEffectivePlan(
            Optional<com.yourco.saas.domain.billing.Subscription> currentSubscription,
            TenantRecord tenant) {
        return resolveEffectivePlanTier(currentSubscription, tenant).name();
    }

    @Transactional
    public void reconcileCustomerSubscriptions(Long customerId, String stripeCustomerId) {
        if (customerId == null || stripeCustomerId == null || stripeCustomerId.isBlank()) {
            return;
        }

        Optional<TenantRecord> tenantOpt = tenantRegistryService.findByStripeCustomerId(stripeCustomerId);
        if (tenantOpt.isEmpty()) {
            log.warn("Cannot reconcile subscriptions: no tenant found for Stripe customer {}", stripeCustomerId);
            return;
        }
        TenantRecord tenant = tenantOpt.get();

        List<com.yourco.saas.domain.billing.Subscription> subscriptions =
                subscriptionRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId);

        Optional<com.yourco.saas.domain.billing.Subscription> currentSubOpt =
                findCurrentSubscription(subscriptions);

        PlanTier effectiveTier = resolveEffectivePlanTier(currentSubOpt, tenant);

        log.info("Reconciled customer {} (tenant {}): effective plan is {}",
                stripeCustomerId, tenant.tenantId(), effectiveTier);

        tenantRegistryService.updatePlan(tenant.tenantId(), effectiveTier.name());
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
        List<SubscriptionItem> items = stripeSubscription.getItems() != null && stripeSubscription.getItems().getData() != null
                ? stripeSubscription.getItems().getData()
                : List.of();
        if (!items.isEmpty()) {
            SubscriptionItem item = items.get(0);
            if (item.getPrice() != null) {
                subscription.setStripePriceId(item.getPrice().getId());
                PlanTier tier = stripeProperties.getTierForPriceId(item.getPrice().getId())
                        .orElse(null);
                if (tier != null) {
                    subscription.setPlanTier(tier);
                } else {
                    log.warn("No plan tier configured for Stripe price {} - check app.stripe.price-tiers", item.getPrice().getId());
                }
            }
            if (item.getCurrentPeriodStart() != null) {
                subscription.setCurrentPeriodStart(Instant.ofEpochSecond(item.getCurrentPeriodStart()));
            }
            if (item.getCurrentPeriodEnd() != null) {
                subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(item.getCurrentPeriodEnd()));
            }
        }

        subscriptionRepository.save(subscription);

        reconcileCustomerSubscriptions(customer.getId(), stripeSubscription.getCustomer());
    }

    @Transactional
    public void handleSubscriptionDeleted(Subscription stripeSubscription) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId())
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.CANCELED);
                    subscriptionRepository.save(subscription);
                });

        Customer customer = customerRepository.findByStripeCustomerId(stripeSubscription.getCustomer())
                .orElse(null);
        if (customer != null) {
            reconcileCustomerSubscriptions(customer.getId(), stripeSubscription.getCustomer());
        } else {
            tenantRegistryService.findByStripeCustomerId(stripeSubscription.getCustomer())
                    .ifPresent(t -> tenantRegistryService.updatePlan(t.tenantId(), PlanTier.FREE.name()));
        }
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