package com.yourco.saas.billing;

import com.stripe.model.Price;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.net.Webhook;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.billing.dto.BillingSummaryResponse;
import com.yourco.saas.billing.dto.CreateCheckoutSessionRequest;
import com.yourco.saas.billing.dto.FeatureEntitlementsResponse;
import com.yourco.saas.domain.billing.Customer;
import com.yourco.saas.domain.billing.CustomerRepository;
import com.yourco.saas.domain.billing.Feature;
import com.yourco.saas.domain.billing.Invoice;
import com.yourco.saas.domain.billing.InvoiceRepository;
import com.yourco.saas.domain.billing.InvoiceStatus;
import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.billing.Subscription;
import com.yourco.saas.domain.billing.SubscriptionRepository;
import com.yourco.saas.domain.billing.SubscriptionStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class SubscriptionLifecycleRegressionTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    BillingService billingService;

    @Autowired
    FeatureEntitlementService featureEntitlementService;

    @Autowired
    StripeProperties stripeProperties;

    @MockitoBean
    StripeCheckoutSessionCreator checkoutSessionCreator;

    @MockitoBean
    StripePortalSessionCreator portalSessionCreator;

    private String loginAs(TenantRecord tenant, Role role, String emailPrefix) {
        String email = emailPrefix + "_" + role.name().toLowerCase() + "@reconciliation.test";
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), email, "password123"), TokenResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        return loginResponse.getBody().accessToken();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // 1. Multiple subscriptions for one customer:
    //    - repository returns List
    //    - BillingService does not throw NonUniqueResultException
    //    - /api/billing returns 200
    @Test
    void multipleSubscriptionsForOneCustomer_BillingReturns200AndDoesNotThrowNonUniqueResultException() {
        TenantRecord tenant = provisionTenant("multi-sub-200");
        String stripeCustId = "cus_multi_sub_200";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "PRO");

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            // Create 3 historical subscriptions
            for (int i = 1; i <= 3; i++) {
                Subscription sub = new Subscription();
                sub.setCustomerId(customer.getId());
                sub.setStripeSubscriptionId("sub_multi_test_" + i);
                sub.setPlanTier(PlanTier.PRO);
                sub.setStatus(i == 3 ? SubscriptionStatus.ACTIVE : SubscriptionStatus.CANCELED);
                sub.setCurrentPeriodStart(Instant.now().minus(30 - i * 10, ChronoUnit.DAYS));
                sub.setCurrentPeriodEnd(Instant.now().plus(i * 10, ChronoUnit.DAYS));
                subscriptionRepository.save(sub);
            }

            List<Subscription> subs = subscriptionRepository.findByCustomerIdOrderByUpdatedAtDesc(customer.getId());
            assertEquals(3, subs.size(), "Repository must return all rows as a list");
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN, "admin1");
        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Must not return 500 when multiple subscription rows exist");
        assertNotNull(response.getBody());
        assertEquals("PRO", response.getBody().plan());
        assertNotNull(response.getBody().subscription());
        assertEquals("sub_multi_test_3", response.getBody().subscription().stripeSubscriptionId());
    }

    // 2. Multiple historical subscriptions + one current subscription:
    //    - current plan is returned
    //    - old subscription does not override it
    @Test
    void multipleHistoricalSubscriptionsPlusOneCurrent_ReturnsCurrentPlan() {
        TenantRecord tenant = provisionTenant("hist-current");
        String stripeCustId = "cus_hist_current_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "ENTERPRISE");

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            // Older canceled PRO subscription
            Subscription oldSub = new Subscription();
            oldSub.setCustomerId(customer.getId());
            oldSub.setStripeSubscriptionId("sub_old_pro");
            oldSub.setPlanTier(PlanTier.PRO);
            oldSub.setStatus(SubscriptionStatus.CANCELED);
            oldSub.setCurrentPeriodStart(Instant.now().minus(60, ChronoUnit.DAYS));
            oldSub.setCurrentPeriodEnd(Instant.now().minus(30, ChronoUnit.DAYS));
            subscriptionRepository.save(oldSub);

            // Newer active ENTERPRISE subscription
            Subscription newSub = new Subscription();
            newSub.setCustomerId(customer.getId());
            newSub.setStripeSubscriptionId("sub_current_enterprise");
            newSub.setPlanTier(PlanTier.ENTERPRISE);
            newSub.setStatus(SubscriptionStatus.ACTIVE);
            newSub.setCurrentPeriodStart(Instant.now().minus(5, ChronoUnit.DAYS));
            newSub.setCurrentPeriodEnd(Instant.now().plus(25, ChronoUnit.DAYS));
            subscriptionRepository.save(newSub);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN, "admin2");
        ResponseEntity<BillingSummaryResponse> summaryRes = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, summaryRes.getStatusCode());
        assertEquals("ENTERPRISE", summaryRes.getBody().plan());
        assertEquals("sub_current_enterprise", summaryRes.getBody().subscription().stripeSubscriptionId());

        // Entitlements must also reflect ENTERPRISE
        ResponseEntity<FeatureEntitlementsResponse> entRes = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), FeatureEntitlementsResponse.class);
        assertEquals(HttpStatus.OK, entRes.getStatusCode());
        assertEquals("ENTERPRISE", entRes.getBody().plan());
        assertTrue(entRes.getBody().features().contains(Feature.CUSTOM_WORKFLOWS));
    }

    // 3. Multiple active local rows caused by historical/test state:
    //    - deterministic current-subscription resolution
    //    - no 500
    //    - no arbitrary Optional single-result query
    //    (Exact simulation of the 4 active rows on tenant_parth_upadhyay)
    @Test
    void multipleActiveLocalRows_ResolvesDeterministicallyWithout500() {
        TenantRecord tenant = provisionTenant("multi-active");
        String stripeCustId = "cus_multi_active_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "PRO");

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            // 4 ACTIVE rows with increasing period timestamps
            Subscription sub1 = new Subscription();
            sub1.setCustomerId(customer.getId());
            sub1.setStripeSubscriptionId("sub_active_1");
            sub1.setPlanTier(PlanTier.PRO);
            sub1.setStatus(SubscriptionStatus.ACTIVE);
            sub1.setCurrentPeriodStart(Instant.ofEpochSecond(1700001000L));
            subscriptionRepository.save(sub1);

            Subscription sub2 = new Subscription();
            sub2.setCustomerId(customer.getId());
            sub2.setStripeSubscriptionId("sub_active_2");
            sub2.setPlanTier(PlanTier.ENTERPRISE);
            sub2.setStatus(SubscriptionStatus.ACTIVE);
            sub2.setCurrentPeriodStart(Instant.ofEpochSecond(1700002000L));
            subscriptionRepository.save(sub2);

            Subscription sub3 = new Subscription();
            sub3.setCustomerId(customer.getId());
            sub3.setStripeSubscriptionId("sub_active_3");
            sub3.setPlanTier(PlanTier.PRO);
            sub3.setStatus(SubscriptionStatus.ACTIVE);
            sub3.setCurrentPeriodStart(Instant.ofEpochSecond(1700003000L));
            subscriptionRepository.save(sub3);

            Subscription sub4 = new Subscription();
            sub4.setCustomerId(customer.getId());
            sub4.setStripeSubscriptionId("sub_active_4");
            sub4.setPlanTier(PlanTier.PRO);
            sub4.setStatus(SubscriptionStatus.ACTIVE);
            sub4.setCurrentPeriodStart(Instant.ofEpochSecond(1700004000L));
            subscriptionRepository.save(sub4);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN, "admin3");
        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRO", response.getBody().plan());
        assertEquals("sub_active_4", response.getBody().subscription().stripeSubscriptionId(),
                "Must deterministically select the newest active subscription");
    }

    // 4. Subscription created/updated:
    //    - current subscription becomes effective
    //    - tenant plan is synchronized to effective plan
    @Test
    void subscriptionCreatedOrUpdated_CurrentSubscriptionBecomesEffective() {
        TenantRecord tenant = provisionTenant("sub-upsert");
        String stripeCustId = "cus_upsert_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);

        TenantContext.setTenant(tenant.schemaName());
        try {
            com.stripe.model.Subscription stripeSub = new com.stripe.model.Subscription();
            stripeSub.setId("sub_webhook_pro");
            stripeSub.setCustomer(stripeCustId);
            stripeSub.setStatus("active");
            stripeSub.setCancelAtPeriodEnd(false);

            SubscriptionItem item = new SubscriptionItem();
            Price price = new Price();
            price.setId("price_test_pro");
            item.setPrice(price);
            item.setCurrentPeriodStart(1700000000L);
            item.setCurrentPeriodEnd(1702592000L);

            SubscriptionItemCollection items = new SubscriptionItemCollection();
            items.setData(List.of(item));
            stripeSub.setItems(items);

            billingService.handleSubscriptionUpsert(stripeSub);
        } finally {
            TenantContext.clear();
        }

        // Check tenant registry plan was synchronized to PRO
        assertEquals("PRO", tenantRegistryService.findByTenantId(tenant.tenantId()).orElseThrow().plan());

        // Check billing summary returns PRO
        String token = loginAs(tenant, Role.ADMIN, "admin4");
        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PRO", response.getBody().plan());
    }

    // 5. Subscription deleted:
    //    - deleted subscription becomes CANCELED
    //    - remaining valid subscription is considered
    //    - FREE only when no valid current subscription remains
    @Test
    void subscriptionDeleted_RemainingValidSubscriptionConsidered_FreeWhenNoneRemain() {
        TenantRecord tenant = provisionTenant("sub-deleted");
        String stripeCustId = "cus_del_test_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "ENTERPRISE");

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            // Sub 1: Older PRO subscription (ACTIVE)
            Subscription sub1 = new Subscription();
            sub1.setCustomerId(customer.getId());
            sub1.setStripeSubscriptionId("sub_del_1_pro");
            sub1.setPlanTier(PlanTier.PRO);
            sub1.setStatus(SubscriptionStatus.ACTIVE);
            sub1.setCurrentPeriodStart(Instant.ofEpochSecond(1700001000L));
            subscriptionRepository.save(sub1);

            // Sub 2: Newer ENTERPRISE subscription (ACTIVE)
            Subscription sub2 = new Subscription();
            sub2.setCustomerId(customer.getId());
            sub2.setStripeSubscriptionId("sub_del_2_ent");
            sub2.setPlanTier(PlanTier.ENTERPRISE);
            sub2.setStatus(SubscriptionStatus.ACTIVE);
            sub2.setCurrentPeriodStart(Instant.ofEpochSecond(1700002000L));
            subscriptionRepository.save(sub2);

            // 1. Delete Sub 2: Sub 1 remains active
            com.stripe.model.Subscription stripeSub2 = new com.stripe.model.Subscription();
            stripeSub2.setId("sub_del_2_ent");
            stripeSub2.setCustomer(stripeCustId);
            billingService.handleSubscriptionDeleted(stripeSub2);

            // Sub 2 is now CANCELED, but Sub 1 is still ACTIVE -> Plan becomes PRO, NOT FREE!
            Subscription sub2After = subscriptionRepository.findByStripeSubscriptionId("sub_del_2_ent").orElseThrow();
            assertEquals(SubscriptionStatus.CANCELED, sub2After.getStatus());
            assertEquals("PRO", tenantRegistryService.findByTenantId(tenant.tenantId()).orElseThrow().plan());

            // 2. Delete Sub 1: No active subscriptions remain
            com.stripe.model.Subscription stripeSub1 = new com.stripe.model.Subscription();
            stripeSub1.setId("sub_del_1_pro");
            stripeSub1.setCustomer(stripeCustId);
            billingService.handleSubscriptionDeleted(stripeSub1);

            // Now tenant becomes FREE
            assertEquals("FREE", tenantRegistryService.findByTenantId(tenant.tenantId()).orElseThrow().plan());
        } finally {
            TenantContext.clear();
        }
    }

    // 6. Cancel-at-period-end:
    //    - remains distinct from immediate cancellation
    //    - still grants plan entitlement
    @Test
    void cancelAtPeriodEnd_RemainsDistinctFromImmediateCancellation() {
        TenantRecord tenant = provisionTenant("cancel-period-end");
        String stripeCustId = "cus_cpe_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "PRO");

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            Subscription sub = new Subscription();
            sub.setCustomerId(customer.getId());
            sub.setStripeSubscriptionId("sub_cpe_pro");
            sub.setPlanTier(PlanTier.PRO);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCancelAtPeriodEnd(true);
            sub.setCurrentPeriodStart(Instant.now().minus(5, ChronoUnit.DAYS));
            sub.setCurrentPeriodEnd(Instant.now().plus(25, ChronoUnit.DAYS));
            subscriptionRepository.save(sub);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN, "admin6");

        // Billing summary reflects ACTIVE with cancelAtPeriodEnd=true
        ResponseEntity<BillingSummaryResponse> summaryRes = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);
        assertEquals(HttpStatus.OK, summaryRes.getStatusCode());
        assertEquals("PRO", summaryRes.getBody().plan());
        assertEquals(SubscriptionStatus.ACTIVE, summaryRes.getBody().subscription().status());
        assertTrue(summaryRes.getBody().subscription().cancelAtPeriodEnd());

        // Entitlements still reflect PRO
        ResponseEntity<FeatureEntitlementsResponse> entRes = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), FeatureEntitlementsResponse.class);
        assertEquals(HttpStatus.OK, entRes.getStatusCode());
        assertEquals("PRO", entRes.getBody().plan());
        assertTrue(entRes.getBody().features().contains(Feature.ADVANCED_REPORTS));
    }

    // 7. Plan change:
    //    - tenant plan changes correctly
    @Test
    void planChange_TenantPlanChangesCorrectly() {
        TenantRecord tenant = provisionTenant("plan-change");
        String stripeCustId = "cus_pc_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);

        TenantContext.setTenant(tenant.schemaName());
        try {
            // 1. Initial subscription: PRO
            com.stripe.model.Subscription stripeSub = new com.stripe.model.Subscription();
            stripeSub.setId("sub_pc_1");
            stripeSub.setCustomer(stripeCustId);
            stripeSub.setStatus("active");
            stripeSub.setCancelAtPeriodEnd(false);

            SubscriptionItem item1 = new SubscriptionItem();
            Price price1 = new Price();
            price1.setId("price_test_pro");
            item1.setPrice(price1);
            item1.setCurrentPeriodStart(1700000000L);
            item1.setCurrentPeriodEnd(1702592000L);

            SubscriptionItemCollection items1 = new SubscriptionItemCollection();
            items1.setData(List.of(item1));
            stripeSub.setItems(items1);

            billingService.handleSubscriptionUpsert(stripeSub);
            assertEquals("PRO", tenantRegistryService.findByTenantId(tenant.tenantId()).orElseThrow().plan());

            // 2. Plan update webhook: upgraded to ENTERPRISE
            SubscriptionItem item2 = new SubscriptionItem();
            Price price2 = new Price();
            price2.setId("price_test_enterprise");
            item2.setPrice(price2);
            item2.setCurrentPeriodStart(1700005000L);
            item2.setCurrentPeriodEnd(1702592000L);

            SubscriptionItemCollection items2 = new SubscriptionItemCollection();
            items2.setData(List.of(item2));
            stripeSub.setItems(items2);

            billingService.handleSubscriptionUpsert(stripeSub);
            assertEquals("ENTERPRISE", tenantRegistryService.findByTenantId(tenant.tenantId()).orElseThrow().plan());
        } finally {
            TenantContext.clear();
        }
    }

    // 8. Webhook ordering:
    //    - stale/older webhook cannot incorrectly overwrite newer effective subscription state
    @Test
    void webhookOrdering_StaleOlderWebhookCannotOverwriteNewerEffectiveSubscription() {
        TenantRecord tenant = provisionTenant("wh-order");
        String stripeCustId = "cus_who_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            // Older subscription: PRO
            Subscription oldSub = new Subscription();
            oldSub.setCustomerId(customer.getId());
            oldSub.setStripeSubscriptionId("sub_older_pro");
            oldSub.setPlanTier(PlanTier.PRO);
            oldSub.setStatus(SubscriptionStatus.ACTIVE);
            oldSub.setCurrentPeriodStart(Instant.ofEpochSecond(1700001000L));
            subscriptionRepository.save(oldSub);

            // Newer subscription: ENTERPRISE
            Subscription newSub = new Subscription();
            newSub.setCustomerId(customer.getId());
            newSub.setStripeSubscriptionId("sub_newer_ent");
            newSub.setPlanTier(PlanTier.ENTERPRISE);
            newSub.setStatus(SubscriptionStatus.ACTIVE);
            newSub.setCurrentPeriodStart(Instant.ofEpochSecond(1700005000L));
            subscriptionRepository.save(newSub);

            tenantRegistryService.updatePlan(tenant.tenantId(), "ENTERPRISE");

            // Now a stale webhook arrives for the older subscription (sub_older_pro)
            com.stripe.model.Subscription staleStripeSub = new com.stripe.model.Subscription();
            staleStripeSub.setId("sub_older_pro");
            staleStripeSub.setCustomer(stripeCustId);
            staleStripeSub.setStatus("active");
            staleStripeSub.setCancelAtPeriodEnd(false);

            SubscriptionItem item = new SubscriptionItem();
            Price price = new Price();
            price.setId("price_test_pro");
            item.setPrice(price);
            item.setCurrentPeriodStart(1700001000L);
            item.setCurrentPeriodEnd(1702592000L);

            SubscriptionItemCollection items = new SubscriptionItemCollection();
            items.setData(List.of(item));
            staleStripeSub.setItems(items);

            billingService.handleSubscriptionUpsert(staleStripeSub);

            // The tenant plan must remain ENTERPRISE, not downgraded to PRO!
            assertEquals("ENTERPRISE", tenantRegistryService.findByTenantId(tenant.tenantId()).orElseThrow().plan(),
                    "Stale older webhook must not downgrade tenant plan");
        } finally {
            TenantContext.clear();
        }
    }

    // 9. Webhook idempotency:
    //    - duplicate webhook does not create duplicate local subscription rows
    @Test
    void webhookIdempotency_DuplicateWebhookDoesNotDuplicateRows() throws Exception {
        TenantRecord tenant = provisionTenant("wh-idempotent");
        String stripeCustId = "cus_idem_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);

        String eventId = "evt_test_idem_" + tenant.tenantId();
        String payload = """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2020-08-27",
                  "type": "customer.subscription.created",
                  "data": {
                    "object": {
                      "id": "sub_idem_%s",
                      "object": "subscription",
                      "customer": "%s",
                      "status": "active",
                      "cancel_at_period_end": false,
                      "items": {
                        "object": "list",
                        "data": [
                          {
                            "id": "si_test",
                            "price": {"id": "price_test_pro"},
                            "current_period_start": 1700000000,
                            "current_period_end": 1702592000
                          }
                        ]
                      }
                    }
                  }
                }
                """.formatted(eventId, tenant.tenantId(), stripeCustId);

        String signature = Webhook.Signature.generateSignatureHeader(payload, stripeProperties.getWebhookSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Stripe-Signature", signature);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // First delivery: OK
        ResponseEntity<String> res1 = restTemplate.exchange(
                "/api/webhooks/stripe", HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        assertEquals("ok", res1.getBody());

        // Second delivery: already processed
        ResponseEntity<String> res2 = restTemplate.exchange(
                "/api/webhooks/stripe", HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
        assertEquals(HttpStatus.OK, res2.getStatusCode());
        assertEquals("already processed", res2.getBody());

        // Check exact single row in database
        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer cust = customerRepository.findByStripeCustomerId(stripeCustId).orElseThrow();
            List<Subscription> subs = subscriptionRepository.findByCustomerIdOrderByUpdatedAtDesc(cust.getId());
            assertEquals(1, subs.size(), "Duplicate webhook delivery must not create multiple rows");
        } finally {
            TenantContext.clear();
        }
    }

    // 10. Cross-tenant isolation:
    //     - subscriptions belonging to another tenant/customer cannot affect current tenant
    @Test
    void crossTenantIsolation_SubscriptionsBelongingToAnotherCannotAffectCurrentTenant() {
        TenantRecord tenantA = provisionTenant("iso-a");
        String custA = "cus_iso_A";
        tenantRegistryService.updateStripeCustomerId(tenantA.tenantId(), custA);
        tenantRegistryService.updatePlan(tenantA.tenantId(), "PRO");

        TenantContext.setTenant(tenantA.schemaName());
        try {
            Customer customerA = new Customer();
            customerA.setStripeCustomerId(custA);
            customerA = customerRepository.save(customerA);

            Subscription subA = new Subscription();
            subA.setCustomerId(customerA.getId());
            subA.setStripeSubscriptionId("sub_tenant_A");
            subA.setPlanTier(PlanTier.PRO);
            subA.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subA);
        } finally {
            TenantContext.clear();
        }

        TenantRecord tenantB = provisionTenant("iso-b");
        String custB = "cus_iso_B";
        tenantRegistryService.updateStripeCustomerId(tenantB.tenantId(), custB);
        tenantRegistryService.updatePlan(tenantB.tenantId(), "FREE");

        TenantContext.setTenant(tenantB.schemaName());
        try {
            Customer customerB = new Customer();
            customerB.setStripeCustomerId(custB);
            customerRepository.save(customerB);
            // Tenant B has NO subscriptions
        } finally {
            TenantContext.clear();
        }

        String tokenB = loginAs(tenantB, Role.ADMIN, "adminB");

        // Tenant B admin cannot see Tenant A's subscription
        ResponseEntity<BillingSummaryResponse> resB = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(tokenB)), BillingSummaryResponse.class);
        assertEquals(HttpStatus.OK, resB.getStatusCode());
        assertEquals("FREE", resB.getBody().plan());
        assertNull(resB.getBody().subscription());
    }

    // 11. Entitlements agreement:
    //     - /api/billing/entitlements agrees with BillingSummary's effective plan
    @Test
    void entitlementsAgreesWithBillingSummaryPlanAcrossStates() {
        TenantRecord tenant = provisionTenant("agree-states");
        String stripeCustId = "cus_agree_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "PRO");

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            Subscription sub = new Subscription();
            sub.setCustomerId(customer.getId());
            sub.setStripeSubscriptionId("sub_agree_pro");
            sub.setPlanTier(PlanTier.PRO);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(sub);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN, "admin11");

        // While ACTIVE: PRO
        ResponseEntity<BillingSummaryResponse> summaryRes = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);
        ResponseEntity<FeatureEntitlementsResponse> entRes = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), FeatureEntitlementsResponse.class);

        assertEquals("PRO", summaryRes.getBody().plan());
        assertEquals("PRO", entRes.getBody().plan());
        assertTrue(entRes.getBody().features().contains(Feature.ADVANCED_REPORTS));

        // When CANCELED: summary becomes FREE, entitlements becomes STARTER
        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = customerRepository.findByStripeCustomerId(stripeCustId).orElseThrow();
            Subscription sub = billingService.findCurrentSubscription(customer.getId()).orElseThrow();
            sub.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(sub);
            tenantRegistryService.updatePlan(tenant.tenantId(), "FREE");
        } finally {
            TenantContext.clear();
        }

        summaryRes = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);
        entRes = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), FeatureEntitlementsResponse.class);

        assertEquals("FREE", summaryRes.getBody().plan());
        assertEquals("STARTER", entRes.getBody().plan());
        assertFalse(entRes.getBody().features().contains(Feature.ADVANCED_REPORTS));
    }

    // 12. Checkout session duplication prevention:
    //     - tenant with an active subscription receives 400 when attempting to create another checkout session
    @Test
    void checkoutSessionBlockedWhenActiveSubscriptionExists() {
        TenantRecord tenant = provisionTenant("checkout-block");
        String stripeCustId = "cus_block_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "PRO");

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            Subscription sub = new Subscription();
            sub.setCustomerId(customer.getId());
            sub.setStripeSubscriptionId("sub_block_pro");
            sub.setPlanTier(PlanTier.PRO);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(sub);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN, "admin12");
        HttpHeaders headers = createAuthHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateCheckoutSessionRequest> entity =
                new HttpEntity<>(new CreateCheckoutSessionRequest(PlanTier.ENTERPRISE), headers);

        // Attempt checkout while subscription is ACTIVE -> 400 Bad Request
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/billing/checkout-session", HttpMethod.POST, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Tenant already has an active subscription"));

        // After subscription is CANCELED, checkout is permitted again
        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = customerRepository.findByStripeCustomerId(stripeCustId).orElseThrow();
            Subscription sub = billingService.findCurrentSubscription(customer.getId()).orElseThrow();
            sub.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(sub);
            tenantRegistryService.updatePlan(tenant.tenantId(), "FREE");
        } finally {
            TenantContext.clear();
        }

        com.stripe.model.checkout.Session fakeSession = new com.stripe.model.checkout.Session();
        fakeSession.setUrl("https://checkout.stripe.com/test-after-cancel");
        try {
            org.mockito.Mockito.when(checkoutSessionCreator.create(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(fakeSession);
        } catch (Exception ignored) {}

        ResponseEntity<String> allowedResponse = restTemplate.exchange(
                "/api/billing/checkout-session", HttpMethod.POST, entity, String.class);
        assertEquals(HttpStatus.OK, allowedResponse.getStatusCode());
    }

    // 13. Invoice synchronization continues to work alongside multiple subscriptions
    @Test
    void invoiceSynchronizationPreservedWithMultipleSubscriptions() {
        TenantRecord tenant = provisionTenant("inv-sync");
        String stripeCustId = "cus_inv_sync_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustId);

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustId);
            customer = customerRepository.save(customer);

            // 2 subscriptions
            Subscription sub1 = new Subscription();
            sub1.setCustomerId(customer.getId());
            sub1.setStripeSubscriptionId("sub_inv_1");
            sub1.setPlanTier(PlanTier.PRO);
            sub1.setStatus(SubscriptionStatus.CANCELED);
            sub1 = subscriptionRepository.save(sub1);

            Subscription sub2 = new Subscription();
            sub2.setCustomerId(customer.getId());
            sub2.setStripeSubscriptionId("sub_inv_2");
            sub2.setPlanTier(PlanTier.ENTERPRISE);
            sub2.setStatus(SubscriptionStatus.ACTIVE);
            sub2 = subscriptionRepository.save(sub2);

            // 2 invoices
            Invoice inv1 = new Invoice();
            inv1.setCustomerId(customer.getId());
            inv1.setSubscriptionId(sub1.getId());
            inv1.setStripeInvoiceId("in_hist_1");
            inv1.setStatus(InvoiceStatus.PAID);
            inv1.setAmountDueCents(2900L);
            inv1.setAmountPaidCents(2900L);
            inv1.setCurrency("usd");
            invoiceRepository.save(inv1);

            Invoice inv2 = new Invoice();
            inv2.setCustomerId(customer.getId());
            inv2.setSubscriptionId(sub2.getId());
            inv2.setStripeInvoiceId("in_hist_2");
            inv2.setStatus(InvoiceStatus.PAID);
            inv2.setAmountDueCents(9900L);
            inv2.setAmountPaidCents(9900L);
            inv2.setCurrency("usd");
            invoiceRepository.save(inv2);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN, "admin13");
        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().invoices());
        assertEquals(2, response.getBody().invoices().size());
    }
}
