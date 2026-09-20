package com.yourco.saas.billing;

import com.yourco.saas.billing.dto.FeatureEntitlementsResponse;
import com.yourco.saas.domain.billing.Customer;
import com.yourco.saas.domain.billing.CustomerRepository;
import com.yourco.saas.domain.billing.Feature;
import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.billing.Subscription;
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service("featureEntitlementService")
public class FeatureEntitlementService {

    private static final Logger log = LoggerFactory.getLogger(FeatureEntitlementService.class);

    private static final Set<Feature> STARTER_FEATURES = Collections.unmodifiableSet(EnumSet.of(
            Feature.EMPLOYEE_MANAGEMENT,
            Feature.TEAM_MANAGEMENT,
            Feature.PROJECT_MANAGEMENT,
            Feature.TASK_MANAGEMENT,
            Feature.CLAIMS,
            Feature.TIME_TRACKING,
            Feature.ATTENDANCE,
            Feature.LEAVE_MANAGEMENT,
            Feature.WORK_SCHEDULES,
            Feature.DOCUMENTS,
            Feature.BASIC_REPORTS,
            Feature.TEAM_CHAT,
            Feature.NOTIFICATIONS,
            Feature.CALENDAR
    ));

    private static final Set<Feature> PRO_FEATURES;
    static {
        EnumSet<Feature> pro = EnumSet.copyOf(STARTER_FEATURES);
        pro.add(Feature.ADVANCED_REPORTS);
        pro.add(Feature.ADVANCED_ANALYTICS);
        pro.add(Feature.ADVANCED_HRM);
        PRO_FEATURES = Collections.unmodifiableSet(pro);
    }

    private static final Set<Feature> ENTERPRISE_FEATURES;
    static {
        EnumSet<Feature> ent = EnumSet.copyOf(PRO_FEATURES);
        ent.add(Feature.CUSTOM_WORKFLOWS);
        ent.add(Feature.ADVANCED_INTEGRATIONS);
        ENTERPRISE_FEATURES = Collections.unmodifiableSet(ent);
    }

    private static final Map<String, Set<Feature>> PLAN_MATRIX = Map.of(
            "FREE", STARTER_FEATURES,
            "STARTER", STARTER_FEATURES,
            "PRO", PRO_FEATURES,
            "ENTERPRISE", ENTERPRISE_FEATURES
    );

    private final TenantRegistryService tenantRegistryService;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingService billingService;

    public FeatureEntitlementService(TenantRegistryService tenantRegistryService,
                                     CustomerRepository customerRepository,
                                     SubscriptionRepository subscriptionRepository,
                                     BillingService billingService) {
        this.tenantRegistryService = tenantRegistryService;
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.billingService = billingService;
    }

    public Set<Feature> getFeaturesForPlan(String plan) {
        if (plan == null) {
            return STARTER_FEATURES;
        }
        return PLAN_MATRIX.getOrDefault(plan.toUpperCase(), STARTER_FEATURES);
    }

    @Transactional(readOnly = true)
    public FeatureEntitlementsResponse getEntitlementsForCurrentTenant() {
        String schema = TenantContext.getTenant();
        if (schema == null || schema.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant context not resolved");
        }

        TenantRecord tenant = tenantRegistryService.findBySchemaName(schema)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant context not resolved"));

        String plan = tenant.plan() != null ? tenant.plan() : PlanTier.FREE.name();
        String status = "ACTIVE";

        if (tenant.stripeCustomerId() != null && !tenant.stripeCustomerId().isBlank()) {
            Optional<Customer> customerOpt = customerRepository.findByStripeCustomerId(tenant.stripeCustomerId());
            if (customerOpt.isPresent()) {
                List<Subscription> subscriptions = subscriptionRepository.findByCustomerIdOrderByUpdatedAtDesc(customerOpt.get().getId());
                Optional<Subscription> subscriptionOpt = billingService.findCurrentSubscription(subscriptions);
                if (subscriptionOpt.isPresent()) {
                    Subscription subscription = subscriptionOpt.get();
                    if (subscription.getStatus() != null) {
                        status = subscription.getStatus().name();
                    }
                    plan = billingService.resolveEffectivePlan(subscriptionOpt, tenant);
                } else {
                    plan = tenant.plan() != null ? tenant.plan() : PlanTier.FREE.name();
                }
            }
        }

        // Normalize FREE to STARTER for client presentation
        String normalizedPlan = ("FREE".equalsIgnoreCase(plan)) ? "STARTER" : plan.toUpperCase();
        Set<Feature> features = getFeaturesForPlan(plan);

        return new FeatureEntitlementsResponse(normalizedPlan, status, features);
    }

    @Transactional(readOnly = true)
    public boolean hasFeature(Feature feature) {
        if (feature == null) {
            return false;
        }
        FeatureEntitlementsResponse entitlements = getEntitlementsForCurrentTenant();
        return entitlements.features().contains(feature);
    }

    @Transactional(readOnly = true)
    public boolean hasFeature(String featureName) {
        try {
            Feature feature = Feature.valueOf(featureName.toUpperCase());
            return hasFeature(feature);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown feature query: {}", featureName);
            return false;
        }
    }
}
