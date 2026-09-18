package com.yourco.saas.billing;

import com.stripe.exception.ApiException;
import com.stripe.model.Price;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.billing.dto.BillingSummaryResponse;
import com.yourco.saas.billing.dto.CreateCheckoutSessionRequest;
import com.yourco.saas.billing.dto.CreateCheckoutSessionResponse;
import com.yourco.saas.billing.dto.CreatePortalSessionResponse;
import com.yourco.saas.domain.billing.Customer;
import com.yourco.saas.domain.billing.CustomerRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
import static org.mockito.Mockito.*;

@AutoConfigureTestRestTemplate
class BillingControllerTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    StripeCheckoutSessionCreator checkoutSessionCreator;

    @MockitoBean
    StripePortalSessionCreator portalSessionCreator;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    StripeProperties stripeProperties;

    @Autowired
    BillingService billingService;

    private String loginAs(TenantRecord tenant, Role role) {
        String email = role.name().toLowerCase() + "@billing.test";
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

    @Test
    void createsCheckoutSessionForAdmin() throws Exception {
        TenantRecord tenant = provisionTenant("billing-checkout");
        String accessToken = loginAs(tenant, Role.ADMIN);

        Session fakeSession = new Session();
        fakeSession.setUrl("https://checkout.stripe.com/test-session-url");
        when(checkoutSessionCreator.create(ArgumentMatchers.any())).thenReturn(fakeSession);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<CreateCheckoutSessionRequest> entity =
                new HttpEntity<>(new CreateCheckoutSessionRequest(PlanTier.PRO), headers);

        ResponseEntity<CreateCheckoutSessionResponse> response = restTemplate.exchange(
                "/api/billing/checkout-session", HttpMethod.POST, entity, CreateCheckoutSessionResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://checkout.stripe.com/test-session-url", response.getBody().checkoutUrl());

        ArgumentCaptor<com.stripe.param.checkout.SessionCreateParams> paramsCaptor =
                ArgumentCaptor.forClass(com.stripe.param.checkout.SessionCreateParams.class);
        verify(checkoutSessionCreator).create(paramsCaptor.capture());
        com.stripe.param.checkout.SessionCreateParams capturedParams = paramsCaptor.getValue();
        assertNotNull(capturedParams.getLineItems());
        assertEquals(1, capturedParams.getLineItems().size());
        assertEquals("price_test_pro", capturedParams.getLineItems().get(0).getPrice());
    }

    @Test
    void createsEnterpriseCheckoutSessionForAdmin() throws Exception {
        TenantRecord tenant = provisionTenant("billing-checkout-ent");
        String accessToken = loginAs(tenant, Role.ADMIN);

        Session fakeSession = new Session();
        fakeSession.setUrl("https://checkout.stripe.com/test-session-url-ent");
        when(checkoutSessionCreator.create(ArgumentMatchers.any())).thenReturn(fakeSession);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<CreateCheckoutSessionRequest> entity =
                new HttpEntity<>(new CreateCheckoutSessionRequest(PlanTier.ENTERPRISE), headers);

        ResponseEntity<CreateCheckoutSessionResponse> response = restTemplate.exchange(
                "/api/billing/checkout-session", HttpMethod.POST, entity, CreateCheckoutSessionResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://checkout.stripe.com/test-session-url-ent", response.getBody().checkoutUrl());

        ArgumentCaptor<com.stripe.param.checkout.SessionCreateParams> paramsCaptor =
                ArgumentCaptor.forClass(com.stripe.param.checkout.SessionCreateParams.class);
        verify(checkoutSessionCreator, atLeastOnce()).create(paramsCaptor.capture());
        com.stripe.param.checkout.SessionCreateParams capturedParams = paramsCaptor.getValue();
        assertNotNull(capturedParams.getLineItems());
        assertEquals(1, capturedParams.getLineItems().size());
        assertEquals("price_test_enterprise", capturedParams.getLineItems().get(0).getPrice());
    }

    @Test
    void adminCanCreatePortalSessionWhenTenantHasStripeCustomer() throws Exception {
        TenantRecord tenant = provisionTenant("portal-admin");
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), "cus_portal_admin_123");
        String token = loginAs(tenant, Role.ADMIN);

        com.stripe.model.billingportal.Session fakeSession = new com.stripe.model.billingportal.Session();
        fakeSession.setUrl("https://billing.stripe.com/p/session_admin");
        when(portalSessionCreator.create(ArgumentMatchers.any())).thenReturn(fakeSession);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<CreatePortalSessionResponse> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(headers), CreatePortalSessionResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("https://billing.stripe.com/p/session_admin", response.getBody().url());
    }

    @Test
    void superAdminCanCreatePortalSession() throws Exception {
        TenantRecord tenant = provisionTenant("portal-super-admin");
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), "cus_portal_super_admin_123");
        String token = loginAs(tenant, Role.SUPER_ADMIN);

        com.stripe.model.billingportal.Session fakeSession = new com.stripe.model.billingportal.Session();
        fakeSession.setUrl("https://billing.stripe.com/p/session_super_admin");
        when(portalSessionCreator.create(ArgumentMatchers.any())).thenReturn(fakeSession);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<CreatePortalSessionResponse> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(headers), CreatePortalSessionResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("https://billing.stripe.com/p/session_super_admin", response.getBody().url());
    }

    @Test
    void managerReceives403ForPortalSession() {
        TenantRecord tenant = provisionTenant("portal-mgr");
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), "cus_portal_mgr_123");
        String token = loginAs(tenant, Role.MANAGER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void userReceives403ForPortalSession() {
        TenantRecord tenant = provisionTenant("portal-usr");
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), "cus_portal_usr_123");
        String token = loginAs(tenant, Role.USER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void tenantWithoutStripeCustomerIdReceivesBadRequestForPortalSession() {
        TenantRecord tenant = provisionTenant("portal-no-cus");
        String token = loginAs(tenant, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void stripePortalSessionIsCreatedForCurrentTenantCustomerId() throws Exception {
        TenantRecord tenant = provisionTenant("portal-params");
        String expectedCustomerId = "cus_target_tenant_abc";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), expectedCustomerId);
        String token = loginAs(tenant, Role.ADMIN);

        com.stripe.model.billingportal.Session fakeSession = new com.stripe.model.billingportal.Session();
        fakeSession.setUrl("https://billing.stripe.com/p/session_params");
        when(portalSessionCreator.create(ArgumentMatchers.any())).thenReturn(fakeSession);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<CreatePortalSessionResponse> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(headers), CreatePortalSessionResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(portalSessionCreator, atLeastOnce()).create(captor.capture());
        SessionCreateParams capturedParams = captor.getValue();
        assertEquals(expectedCustomerId, capturedParams.getCustomer());
        assertEquals(stripeProperties.getPortalReturnUrl(), capturedParams.getReturnUrl());
    }

    @Test
    void stripeFailureTranslatedToBadGatewayForPortalSession() throws Exception {
        TenantRecord tenant = provisionTenant("portal-fail");
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), "cus_portal_fail_123");
        String token = loginAs(tenant, Role.ADMIN);

        when(portalSessionCreator.create(ArgumentMatchers.any()))
                .thenThrow(new ApiException("Stripe portal service down", null, null, 502, null));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void clientCannotOverrideStripeCustomerIdInPortalRequest() throws Exception {
        TenantRecord tenant = provisionTenant("portal-override");
        String genuineCustomerId = "cus_genuine_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), genuineCustomerId);
        String token = loginAs(tenant, Role.ADMIN);

        com.stripe.model.billingportal.Session fakeSession = new com.stripe.model.billingportal.Session();
        fakeSession.setUrl("https://billing.stripe.com/p/session_genuine");
        when(portalSessionCreator.create(ArgumentMatchers.any())).thenReturn(fakeSession);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String maliciousBody = "{\"stripeCustomerId\":\"cus_attacker_controlled_999\",\"customer\":\"cus_attacker\"}";

        ResponseEntity<CreatePortalSessionResponse> response = restTemplate.exchange(
                "/api/billing/portal-session", HttpMethod.POST, new HttpEntity<>(maliciousBody, headers), CreatePortalSessionResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(portalSessionCreator, atLeastOnce()).create(captor.capture());
        assertEquals(genuineCustomerId, captor.getValue().getCustomer());
    }

    @Test
    void adminCanRetrieveBillingInformation() {
        TenantRecord tenant = provisionTenant("summary-admin");
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), "cus_summary_admin");
        String token = loginAs(tenant, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void superAdminCanRetrieveBillingInformation() {
        TenantRecord tenant = provisionTenant("summary-super-admin");
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), "cus_summary_super_admin");
        String token = loginAs(tenant, Role.SUPER_ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void managerReceives403ForBillingSummary() {
        TenantRecord tenant = provisionTenant("summary-mgr");
        String token = loginAs(tenant, Role.MANAGER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void userReceives403ForBillingSummary() {
        TenantRecord tenant = provisionTenant("summary-usr");
        String token = loginAs(tenant, Role.USER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void returnsCorrectPlanSubscriptionAndInvoices() {
        TenantRecord tenant = provisionTenant("summary-data");
        String stripeCustomerId = "cus_summary_data_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustomerId);
        tenantRegistryService.updatePlan(tenant.tenantId(), "PRO");

        Instant periodStart = Instant.now().minus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant periodEnd = Instant.now().plus(25, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant paidAt = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustomerId);
            customer.setBillingEmail("billing@example.test");
            customer = customerRepository.save(customer);

            Subscription subscription = new Subscription();
            subscription.setCustomerId(customer.getId());
            subscription.setPlanTier(PlanTier.PRO);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setStripeSubscriptionId("sub_real_123");
            subscription.setStripePriceId("price_test_pro");
            subscription.setCurrentPeriodStart(periodStart);
            subscription.setCurrentPeriodEnd(periodEnd);
            subscription.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(subscription);

            Invoice invoice = new Invoice();
            invoice.setCustomerId(customer.getId());
            invoice.setSubscriptionId(subscription.getId());
            invoice.setStripeInvoiceId("in_real_123");
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setAmountDueCents(2900L);
            invoice.setAmountPaidCents(2900L);
            invoice.setCurrency("usd");
            invoice.setHostedInvoiceUrl("https://invoice.stripe.com/in_real_123");
            invoice.setInvoicePdfUrl("https://invoice.stripe.com/in_real_123.pdf");
            invoice.setPaidAt(paidAt);
            invoiceRepository.save(invoice);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BillingSummaryResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("PRO", body.plan());

        assertNotNull(body.subscription());
        assertEquals(SubscriptionStatus.ACTIVE, body.subscription().status());
        assertEquals("sub_real_123", body.subscription().stripeSubscriptionId());
        assertEquals("price_test_pro", body.subscription().stripePriceId());
        assertEquals(periodStart, body.subscription().currentPeriodStart());
        assertEquals(periodEnd, body.subscription().currentPeriodEnd());
        assertFalse(body.subscription().cancelAtPeriodEnd());

        assertNotNull(body.invoices());
        assertEquals(1, body.invoices().size());
        var inv = body.invoices().get(0);
        assertEquals("in_real_123", inv.stripeInvoiceId());
        assertEquals(InvoiceStatus.PAID, inv.status());
        assertEquals(2900L, inv.amountDueCents());
        assertEquals(2900L, inv.amountPaidCents());
        assertEquals("usd", inv.currency());
        assertEquals("https://invoice.stripe.com/in_real_123", inv.hostedInvoiceUrl());
        assertEquals("https://invoice.stripe.com/in_real_123.pdf", inv.invoicePdfUrl());
        assertEquals(paidAt, inv.paidAt());
    }

    @Test
    void tenantWithNoStripeCustomerGetsFreeAndNullSubscription() {
        TenantRecord tenant = provisionTenant("summary-no-cus");
        String token = loginAs(tenant, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BillingSummaryResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("FREE", body.plan());
        assertNull(body.subscription());
        assertNotNull(body.invoices());
        assertTrue(body.invoices().isEmpty());
    }

    @Test
    void tenantWithCustomerButNoSubscriptionGetsSubscriptionNull() {
        TenantRecord tenant = provisionTenant("summary-no-sub");
        String stripeCustomerId = "cus_nosub_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustomerId);

        TenantContext.setTenant(tenant.schemaName());
        try {
            Customer customer = new Customer();
            customer.setStripeCustomerId(stripeCustomerId);
            customerRepository.save(customer);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BillingSummaryResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("FREE", body.plan());
        assertNull(body.subscription());
        assertNotNull(body.invoices());
        assertTrue(body.invoices().isEmpty());
    }

    @Test
    void crossTenantBillingIsolation() {
        TenantRecord tenantA = provisionTenant("summary-tenant-a");
        String customerIdA = "cus_tenant_A";
        tenantRegistryService.updateStripeCustomerId(tenantA.tenantId(), customerIdA);
        tenantRegistryService.updatePlan(tenantA.tenantId(), "PRO");

        TenantContext.setTenant(tenantA.schemaName());
        try {
            Customer custA = new Customer();
            custA.setStripeCustomerId(customerIdA);
            custA = customerRepository.save(custA);

            Subscription subA = new Subscription();
            subA.setCustomerId(custA.getId());
            subA.setPlanTier(PlanTier.PRO);
            subA.setStatus(SubscriptionStatus.ACTIVE);
            subA.setStripeSubscriptionId("sub_tenant_A");
            subscriptionRepository.save(subA);
        } finally {
            TenantContext.clear();
        }

        TenantRecord tenantB = provisionTenant("summary-tenant-b");
        String customerIdB = "cus_tenant_B";
        tenantRegistryService.updateStripeCustomerId(tenantB.tenantId(), customerIdB);
        tenantRegistryService.updatePlan(tenantB.tenantId(), "ENTERPRISE");

        TenantContext.setTenant(tenantB.schemaName());
        try {
            Customer custB = new Customer();
            custB.setStripeCustomerId(customerIdB);
            custB = customerRepository.save(custB);

            Subscription subB = new Subscription();
            subB.setCustomerId(custB.getId());
            subB.setPlanTier(PlanTier.ENTERPRISE);
            subB.setStatus(SubscriptionStatus.ACTIVE);
            subB.setStripeSubscriptionId("sub_tenant_B");
            subscriptionRepository.save(subB);
        } finally {
            TenantContext.clear();
        }

        // Login as tenant A admin
        String tokenA = loginAs(tenantA, Role.ADMIN);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);

        // Even with malicious query parameters trying to fetch tenant B's data:
        ResponseEntity<BillingSummaryResponse> responseA = restTemplate.exchange(
                "/api/billing?tenantId=" + tenantB.tenantId() + "&stripeCustomerId=" + customerIdB,
                HttpMethod.GET, new HttpEntity<>(headers), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, responseA.getStatusCode());
        BillingSummaryResponse bodyA = responseA.getBody();
        assertNotNull(bodyA);
        assertEquals("PRO", bodyA.plan());
        assertNotNull(bodyA.subscription());
        assertEquals("sub_tenant_A", bodyA.subscription().stripeSubscriptionId());
        assertNotEquals("sub_tenant_B", bodyA.subscription().stripeSubscriptionId());
    }

    @Test
    void webhookCreatedSubscriptionAndInvoiceRecordsAreCorrectlyRepresented() {
        TenantRecord tenant = provisionTenant("summary-webhook");
        String stripeCustomerId = "cus_wh_test_123";
        tenantRegistryService.updateStripeCustomerId(tenant.tenantId(), stripeCustomerId);

        TenantContext.setTenant(tenant.schemaName());
        try {
            // Simulate webhook processing:
            com.stripe.model.Subscription stripeSubscription = new com.stripe.model.Subscription();
            stripeSubscription.setId("sub_wh_123");
            stripeSubscription.setCustomer(stripeCustomerId);
            stripeSubscription.setStatus("active");
            stripeSubscription.setCancelAtPeriodEnd(false);

            SubscriptionItem item = new SubscriptionItem();
            Price price = new Price();
            price.setId("price_test_pro");
            item.setPrice(price);
            item.setCurrentPeriodStart(1700000000L);
            item.setCurrentPeriodEnd(1702592000L);

            SubscriptionItemCollection items = new SubscriptionItemCollection();
            items.setData(List.of(item));
            stripeSubscription.setItems(items);

            billingService.handleSubscriptionUpsert(stripeSubscription);

            com.stripe.model.Invoice stripeInvoice = new com.stripe.model.Invoice();
            stripeInvoice.setId("in_wh_123");
            stripeInvoice.setCustomer(stripeCustomerId);
            stripeInvoice.setStatus("paid");
            stripeInvoice.setAmountDue(4900L);
            stripeInvoice.setAmountPaid(4900L);
            stripeInvoice.setCurrency("usd");
            stripeInvoice.setHostedInvoiceUrl("https://invoice.stripe.com/wh_123");
            stripeInvoice.setInvoicePdf("https://invoice.stripe.com/wh_123.pdf");

            billingService.handleInvoiceEvent(stripeInvoice);
        } finally {
            TenantContext.clear();
        }

        String token = loginAs(tenant, Role.ADMIN);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<BillingSummaryResponse> response = restTemplate.exchange(
                "/api/billing", HttpMethod.GET, new HttpEntity<>(headers), BillingSummaryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BillingSummaryResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("PRO", body.plan());

        assertNotNull(body.subscription());
        assertEquals("sub_wh_123", body.subscription().stripeSubscriptionId());
        assertEquals("price_test_pro", body.subscription().stripePriceId());
        assertEquals(SubscriptionStatus.ACTIVE, body.subscription().status());

        assertNotNull(body.invoices());
        assertEquals(1, body.invoices().size());
        assertEquals("in_wh_123", body.invoices().get(0).stripeInvoiceId());
        assertEquals(4900L, body.invoices().get(0).amountDueCents());
        assertEquals(4900L, body.invoices().get(0).amountPaidCents());
        assertEquals(InvoiceStatus.PAID, body.invoices().get(0).status());
    }
}