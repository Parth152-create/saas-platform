package com.yourco.saas.billing;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.billing.dto.FeatureEntitlementsResponse;
import com.yourco.saas.domain.billing.Customer;
import com.yourco.saas.domain.billing.CustomerRepository;
import com.yourco.saas.domain.billing.Feature;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class FeatureEntitlementTest extends IntegrationTestBase {

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
    BillingService billingService;

    @Autowired
    FeatureEntitlementService featureEntitlementService;

    private String loginAs(TenantRecord tenant, Role role, String emailPrefix) {
        String email = emailPrefix + "_" + role.name().toLowerCase() + "@entitlements.test";
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

    private void attachSubscription(TenantRecord tenant, String customerId, PlanTier tier) {
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), customerId);
        tenantRegistryService.updatePlan(tenant.tenantId(), tier.name());

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(customerId);
            customer.setBillingEmail("billing@" + tenant.tenantId() + ".test");
            customer = customerRepository.save(customer);

            Subscription subscription = new Subscription();
            subscription.setCustomerId(customer.getId());
            subscription.setPlanTier(tier);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setStripeSubscriptionId("sub_" + tenant.tenantId());
            subscription.setStripePriceId("price_" + tier.name().toLowerCase());
            subscription.setCurrentPeriodStart(Instant.now().minus(5, ChronoUnit.DAYS));
            subscription.setCurrentPeriodEnd(Instant.now().plus(25, ChronoUnit.DAYS));
            subscription.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(subscription);
        } finally {
            TenantContext.clear();
        }
    }

    // 1. Starter tenant receives Starter features
    @Test
    void starterTenantReceivesStarterFeatures() {
        TenantRecord tenant = provisionTenant("ent-starter");
        String token = loginAs(tenant, Role.USER, "u1");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<FeatureEntitlementsResponse> response = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(headers), FeatureEntitlementsResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FeatureEntitlementsResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("STARTER", body.plan());
        assertTrue(body.features().contains(Feature.EMPLOYEE_MANAGEMENT));
        assertTrue(body.features().contains(Feature.BASIC_REPORTS));
        assertFalse(body.features().contains(Feature.ADVANCED_REPORTS));
        assertFalse(body.features().contains(Feature.ADVANCED_ANALYTICS));
        assertFalse(body.features().contains(Feature.CUSTOM_WORKFLOWS));
        assertFalse(body.features().contains(Feature.ADVANCED_INTEGRATIONS));
    }

    // 2. Pro tenant receives Pro features
    @Test
    void proTenantReceivesProFeatures() {
        TenantRecord tenant = provisionTenant("ent-pro");
        attachSubscription(tenant, "cus_pro_123", PlanTier.PRO);
        String token = loginAs(tenant, Role.USER, "u2");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<FeatureEntitlementsResponse> response = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(headers), FeatureEntitlementsResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FeatureEntitlementsResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("PRO", body.plan());
        assertTrue(body.features().contains(Feature.EMPLOYEE_MANAGEMENT));
        assertTrue(body.features().contains(Feature.BASIC_REPORTS));
        assertTrue(body.features().contains(Feature.ADVANCED_REPORTS));
        assertTrue(body.features().contains(Feature.ADVANCED_ANALYTICS));
        assertTrue(body.features().contains(Feature.ADVANCED_HRM));
        assertFalse(body.features().contains(Feature.CUSTOM_WORKFLOWS));
        assertFalse(body.features().contains(Feature.ADVANCED_INTEGRATIONS));
    }

    // 3. Enterprise tenant receives Enterprise features
    @Test
    void enterpriseTenantReceivesEnterpriseFeatures() {
        TenantRecord tenant = provisionTenant("ent-enterprise");
        attachSubscription(tenant, "cus_ent_123", PlanTier.ENTERPRISE);
        String token = loginAs(tenant, Role.USER, "u3");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<FeatureEntitlementsResponse> response = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(headers), FeatureEntitlementsResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FeatureEntitlementsResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("ENTERPRISE", body.plan());
        assertTrue(body.features().contains(Feature.EMPLOYEE_MANAGEMENT));
        assertTrue(body.features().contains(Feature.ADVANCED_REPORTS));
        assertTrue(body.features().contains(Feature.ADVANCED_ANALYTICS));
        assertTrue(body.features().contains(Feature.CUSTOM_WORKFLOWS));
        assertTrue(body.features().contains(Feature.ADVANCED_INTEGRATIONS));
    }

    // 4. Starter cannot access Pro-only APIs
    @Test
    void starterCannotAccessProOnlyApis() {
        TenantRecord tenant = provisionTenant("starter-access");
        String token = loginAs(tenant, Role.USER, "u4");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // Basic report succeeds
        ResponseEntity<String> basicRes = restTemplate.exchange(
                "/api/reports/basic", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, basicRes.getStatusCode());

        // Advanced report is rejected with 403 Forbidden
        ResponseEntity<String> advRes = restTemplate.exchange(
                "/api/reports/advanced", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, advRes.getStatusCode());

        // Analytics is rejected with 403 Forbidden
        ResponseEntity<String> anaRes = restTemplate.exchange(
                "/api/reports/analytics", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, anaRes.getStatusCode());
    }

    // 5. Pro can access Pro-only APIs but not Enterprise
    @Test
    void proCanAccessProOnlyApisButNotEnterprise() {
        TenantRecord tenant = provisionTenant("pro-access");
        attachSubscription(tenant, "cus_pro_acc_123", PlanTier.PRO);
        String token = loginAs(tenant, Role.USER, "u5");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // Basic report succeeds
        ResponseEntity<String> basicRes = restTemplate.exchange(
                "/api/reports/basic", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, basicRes.getStatusCode());

        // Advanced report succeeds
        ResponseEntity<String> advRes = restTemplate.exchange(
                "/api/reports/advanced", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, advRes.getStatusCode());

        // Analytics succeeds
        ResponseEntity<String> anaRes = restTemplate.exchange(
                "/api/reports/analytics", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, anaRes.getStatusCode());

        // Custom workflows (Enterprise only) fails with 403
        ResponseEntity<String> wfRes = restTemplate.exchange(
                "/api/reports/custom-workflows", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, wfRes.getStatusCode());
    }

    // 6. Enterprise can access Enterprise-only APIs
    @Test
    void enterpriseCanAccessEnterpriseOnlyApis() {
        TenantRecord tenant = provisionTenant("ent-access");
        attachSubscription(tenant, "cus_ent_acc_123", PlanTier.ENTERPRISE);
        String token = loginAs(tenant, Role.USER, "u6");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // Custom workflows succeeds
        ResponseEntity<String> wfRes = restTemplate.exchange(
                "/api/reports/custom-workflows", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, wfRes.getStatusCode());
    }

    // 7. Changing subscription changes entitlements
    @Test
    void changingSubscriptionChangesEntitlements() {
        TenantRecord tenant = provisionTenant("change-sub");
        String token = loginAs(tenant, Role.USER, "u7");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // Initially STARTER -> 403
        ResponseEntity<String> initialRes = restTemplate.exchange(
                "/api/reports/advanced", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, initialRes.getStatusCode());

        // Upgrade to PRO
        attachSubscription(tenant, "cus_change_123", PlanTier.PRO);

        // Now PRO -> 200 OK
        ResponseEntity<String> upgradedRes = restTemplate.exchange(
                "/api/reports/advanced", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, upgradedRes.getStatusCode());
    }

    // 8. One tenant cannot use another tenant's subscription
    @Test
    void oneTenantCannotUseAnotherTenantsSubscription() {
        TenantRecord tenantA = provisionTenant("iso-tenant-a"); // STARTER
        TenantRecord tenantB = provisionTenant("iso-tenant-b"); // PRO
        attachSubscription(tenantB, "cus_iso_b_123", PlanTier.PRO);

        String tokenA = loginAs(tenantA, Role.USER, "ua");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);

        // Tenant A tries to access Pro endpoint
        ResponseEntity<String> resA = restTemplate.exchange(
                "/api/reports/advanced", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resA.getStatusCode());
    }

    // 9. Frontend values cannot bypass backend authorization
    @Test
    void frontendValuesCannotBypassBackendAuthorization() {
        TenantRecord tenant = provisionTenant("bypass-test");
        String token = loginAs(tenant, Role.USER, "ubypass");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Tenant-Plan", "ENTERPRISE");
        headers.set("X-Override-Plan", "ENTERPRISE");

        // Attacker attempts to forge plan in query param and headers
        ResponseEntity<String> res = restTemplate.exchange(
                "/api/reports/advanced?plan=ENTERPRISE&tier=ENTERPRISE", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    // 10. Existing RBAC remains functional in combination with feature entitlement
    @Test
    void rbacAndFeatureEntitlementCombinedCheck() {
        TenantRecord tenant = provisionTenant("rbac-feature");
        attachSubscription(tenant, "cus_rbac_123", PlanTier.PRO);

        // USER role with PRO plan:
        String userToken = loginAs(tenant, Role.USER, "u_regular");
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);

        // USER cannot access ADMIN-only advanced export even though PRO feature is entitled
        ResponseEntity<String> userRes = restTemplate.exchange(
                "/api/reports/admin-advanced-export", HttpMethod.POST, new HttpEntity<>(userHeaders), String.class);
        assertEquals(HttpStatus.FORBIDDEN, userRes.getStatusCode());

        // ADMIN role with PRO plan:
        String adminToken = loginAs(tenant, Role.ADMIN, "u_admin");
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);

        // ADMIN can access ADMIN-only advanced export because both RBAC and feature entitlement are satisfied
        ResponseEntity<String> adminRes = restTemplate.exchange(
                "/api/reports/admin-advanced-export", HttpMethod.POST, new HttpEntity<>(adminHeaders), String.class);
        assertEquals(HttpStatus.OK, adminRes.getStatusCode());
    }

    // 11. Cross-tenant isolation remains strictly functional with feature entitlement
    @Test
    void crossTenantIsolationRemainsFunctionalWithFeatureEntitlements() {
        TenantRecord tenantA = provisionTenant("iso-ent-a");
        TenantRecord tenantB = provisionTenant("iso-ent-b");
        attachSubscription(tenantA, "cus_iso_a", PlanTier.PRO);
        attachSubscription(tenantB, "cus_iso_b", PlanTier.PRO);

        String tokenA = loginAs(tenantA, Role.ADMIN, "admin_a");
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);

        // Tenant A admin cannot query users or billing data of Tenant B
        ResponseEntity<String> resA = restTemplate.exchange(
                "/api/billing?tenantId=" + tenantB.tenantId(), HttpMethod.GET, new HttpEntity<>(headersA), String.class);
        assertEquals(HttpStatus.OK, resA.getStatusCode());
        assertFalse(resA.getBody().contains("cus_iso_b"));
    }

    // 12. Subscription cancellation immediately revokes entitlements and blocks Pro APIs
    @Test
    void cancelingSubscriptionRevokesEntitlements() {
        TenantRecord tenant = provisionTenant("cancel-ent");
        attachSubscription(tenant, "cus_cancel_123", PlanTier.PRO);
        String token = loginAs(tenant, Role.USER, "u_cancel");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // While PRO -> can access advanced reports
        ResponseEntity<String> beforeRes = restTemplate.exchange(
                "/api/reports/advanced", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, beforeRes.getStatusCode());

        // Cancel subscription
        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = customerRepository.findByStripeCustomerId("cus_cancel_123").orElseThrow();
            Subscription subscription = billingService.findCurrentSubscription(customer.getId()).orElseThrow();
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(subscription);
            tenantRegistryService.updatePlan(tenant.tenantId(), PlanTier.FREE.name());
        } finally {
            TenantContext.clear();
        }

        // Entitlements endpoint now reflects STARTER
        ResponseEntity<FeatureEntitlementsResponse> entRes = restTemplate.exchange(
                "/api/billing/entitlements", HttpMethod.GET, new HttpEntity<>(headers), FeatureEntitlementsResponse.class);
        assertEquals(HttpStatus.OK, entRes.getStatusCode());
        assertNotNull(entRes.getBody());
        assertEquals("STARTER", entRes.getBody().plan());
        assertFalse(entRes.getBody().features().contains(Feature.ADVANCED_REPORTS));

        // Pro-only endpoint now returns 403 Forbidden
        ResponseEntity<String> afterRes = restTemplate.exchange(
                "/api/reports/advanced", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, afterRes.getStatusCode());
    }
}
