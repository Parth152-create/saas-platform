package com.yourco.saas.billing;

import com.stripe.model.checkout.Session;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.billing.dto.CreateCheckoutSessionRequest;
import com.yourco.saas.billing.dto.CreateCheckoutSessionResponse;
import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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

    @Test
    void createsCheckoutSessionForAdmin() throws Exception {
        TenantRecord tenant = provisionTenant("billing-checkout");

        TenantContext.setTenant(tenant.schemaName());
        User admin = UserTestFactory.localUser("admin@example.test", Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);
        TenantContext.clear();

        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), "admin@example.test", "password123"), TokenResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String accessToken = loginResponse.getBody().accessToken();

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
    }
}