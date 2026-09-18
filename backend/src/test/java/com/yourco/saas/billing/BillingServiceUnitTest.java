package com.yourco.saas.billing;

import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.billing.Subscription;
import com.yourco.saas.domain.billing.SubscriptionStatus;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BillingServiceUnitTest {

    @Test
    void isEntitledStatusReturnsTrueOnlyForEntitledStatuses() {
        assertTrue(BillingService.isEntitledStatus(SubscriptionStatus.ACTIVE));
        assertTrue(BillingService.isEntitledStatus(SubscriptionStatus.TRIALING));
        assertTrue(BillingService.isEntitledStatus(SubscriptionStatus.PAST_DUE));

        assertFalse(BillingService.isEntitledStatus(SubscriptionStatus.PAUSED));
        assertFalse(BillingService.isEntitledStatus(SubscriptionStatus.UNPAID));
        assertFalse(BillingService.isEntitledStatus(SubscriptionStatus.INCOMPLETE));
        assertFalse(BillingService.isEntitledStatus(SubscriptionStatus.INCOMPLETE_EXPIRED));
        assertFalse(BillingService.isEntitledStatus(SubscriptionStatus.CANCELED));
        assertFalse(BillingService.isEntitledStatus(null));
    }

    @Test
    void findCurrentSubscriptionReturnsEmptyForEmptyOrNullList() {
        assertTrue(BillingService.findCurrentSubscription((List<Subscription>) null).isEmpty());
        assertTrue(BillingService.findCurrentSubscription(List.of()).isEmpty());
    }

    @Test
    void findCurrentSubscriptionPrefersEntitledOverCanceled() {
        Subscription sub1 = createSub(1L, "sub_old_canceled", SubscriptionStatus.CANCELED, PlanTier.PRO,
                Instant.ofEpochSecond(1000), LocalDateTime.of(2026, 1, 1, 0, 0));
        Subscription sub2 = createSub(2L, "sub_active", SubscriptionStatus.ACTIVE, PlanTier.ENTERPRISE,
                Instant.ofEpochSecond(2000), LocalDateTime.of(2026, 1, 2, 0, 0));

        Optional<Subscription> result = BillingService.findCurrentSubscription(List.of(sub1, sub2));
        assertTrue(result.isPresent());
        assertEquals("sub_active", result.get().getStripeSubscriptionId());
    }

    @Test
    void findCurrentSubscriptionPrefersEntitledEvenIfCanceledIsNewer() {
        // Active subscription from earlier, and an incomplete/canceled interaction from later
        Subscription sub1 = createSub(1L, "sub_active", SubscriptionStatus.ACTIVE, PlanTier.PRO,
                Instant.ofEpochSecond(1000), LocalDateTime.of(2026, 1, 1, 0, 0));
        Subscription sub2 = createSub(2L, "sub_canceled_later", SubscriptionStatus.CANCELED, PlanTier.ENTERPRISE,
                Instant.ofEpochSecond(2000), LocalDateTime.of(2026, 1, 2, 0, 0));

        Optional<Subscription> result = BillingService.findCurrentSubscription(List.of(sub1, sub2));
        assertTrue(result.isPresent());
        assertEquals("sub_active", result.get().getStripeSubscriptionId());
    }

    @Test
    void findCurrentSubscriptionSelectsNewestAmongMultipleActiveSubscriptions() {
        Subscription sub1 = createSub(1L, "sub_1", SubscriptionStatus.ACTIVE, PlanTier.PRO,
                Instant.ofEpochSecond(1000), LocalDateTime.of(2026, 1, 1, 10, 0));
        Subscription sub2 = createSub(2L, "sub_2", SubscriptionStatus.ACTIVE, PlanTier.ENTERPRISE,
                Instant.ofEpochSecond(2000), LocalDateTime.of(2026, 1, 1, 11, 0));
        Subscription sub3 = createSub(3L, "sub_3", SubscriptionStatus.ACTIVE, PlanTier.PRO,
                Instant.ofEpochSecond(3000), LocalDateTime.of(2026, 1, 1, 12, 0));

        Optional<Subscription> result = BillingService.findCurrentSubscription(List.of(sub1, sub2, sub3));
        assertTrue(result.isPresent());
        assertEquals("sub_3", result.get().getStripeSubscriptionId());
    }

    @Test
    void findCurrentSubscriptionReturnsLatestCanceledWhenAllAreCanceled() {
        Subscription sub1 = createSub(1L, "sub_canc_1", SubscriptionStatus.CANCELED, PlanTier.PRO,
                Instant.ofEpochSecond(1000), LocalDateTime.of(2026, 1, 1, 0, 0));
        Subscription sub2 = createSub(2L, "sub_canc_2", SubscriptionStatus.CANCELED, PlanTier.ENTERPRISE,
                Instant.ofEpochSecond(2000), LocalDateTime.of(2026, 1, 2, 0, 0));

        Optional<Subscription> result = BillingService.findCurrentSubscription(List.of(sub1, sub2));
        assertTrue(result.isPresent());
        assertEquals("sub_canc_2", result.get().getStripeSubscriptionId());
    }

    @Test
    void resolveEffectivePlanTierReturnsFreeForCanceledOrNonEntitledSubscription() {
        BillingService service = new BillingService(null, null, null, null, null, null);
        TenantRecord tenant = new TenantRecord(1L, "t1", "schema1", "ACTIVE", "PRO", "cus_1");

        Subscription canceledSub = createSub(1L, "sub_c", SubscriptionStatus.CANCELED, PlanTier.PRO, null, null);
        assertEquals(PlanTier.FREE, service.resolveEffectivePlanTier(Optional.of(canceledSub), tenant));

        Subscription unpaidSub = createSub(2L, "sub_u", SubscriptionStatus.UNPAID, PlanTier.ENTERPRISE, null, null);
        assertEquals(PlanTier.FREE, service.resolveEffectivePlanTier(Optional.of(unpaidSub), tenant));

        Subscription incompleteSub = createSub(3L, "sub_i", SubscriptionStatus.INCOMPLETE, PlanTier.PRO, null, null);
        assertEquals(PlanTier.FREE, service.resolveEffectivePlanTier(Optional.of(incompleteSub), tenant));
    }

    @Test
    void resolveEffectivePlanTierReturnsSubscriptionTierWhenActiveOrTrialingOrPastDue() {
        BillingService service = new BillingService(null, null, null, null, null, null);
        TenantRecord tenant = new TenantRecord(1L, "t1", "schema1", "ACTIVE", "FREE", "cus_1");

        Subscription activeSub = createSub(1L, "sub_a", SubscriptionStatus.ACTIVE, PlanTier.PRO, null, null);
        assertEquals(PlanTier.PRO, service.resolveEffectivePlanTier(Optional.of(activeSub), tenant));

        Subscription trialingSub = createSub(2L, "sub_t", SubscriptionStatus.TRIALING, PlanTier.ENTERPRISE, null, null);
        assertEquals(PlanTier.ENTERPRISE, service.resolveEffectivePlanTier(Optional.of(trialingSub), tenant));

        Subscription pastDueSub = createSub(3L, "sub_p", SubscriptionStatus.PAST_DUE, PlanTier.PRO, null, null);
        assertEquals(PlanTier.PRO, service.resolveEffectivePlanTier(Optional.of(pastDueSub), tenant));
    }

    private Subscription createSub(Long id, String stripeSubId, SubscriptionStatus status, PlanTier tier,
                                  Instant periodStart, LocalDateTime createdAt) {
        Subscription sub = new Subscription();
        // Use reflection to set id / createdAt if necessary or setter
        sub.setStripeSubscriptionId(stripeSubId);
        sub.setStatus(status);
        sub.setPlanTier(tier);
        sub.setCurrentPeriodStart(periodStart);
        return sub;
    }
}
