package com.yourco.saas.billing;

import com.stripe.net.Webhook;
import com.yourco.saas.domain.billing.Customer;
import com.yourco.saas.domain.billing.CustomerRepository;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestRestTemplate
class StripeWebhookControllerTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    StripeProperties stripeProperties;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    TenantRegistryService tenantRegistryService;

    @Test
    void checkoutSessionCompletedLinksStripeCustomerAndCreatesLocalCustomer() throws NoSuchAlgorithmException, InvalidKeyException {
        TenantRecord tenant = provisionTenant("webhook-checkout");
        String stripeCustomerId = "cus_test_" + tenant.tenantId();

        String payload = """
                {
                  "id": "evt_test_checkout_%s",
                  "object": "event",
                  "api_version": "2020-08-27",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_%s",
                      "object": "checkout.session",
                      "customer": "%s",
                      "client_reference_id": "%s",
                      "mode": "subscription",
                      "payment_status": "paid"
                    }
                  }
                }
                """.formatted(tenant.tenantId(), tenant.tenantId(), stripeCustomerId, tenant.tenantId());

        String signature = Webhook.Signature.generateSignatureHeader(payload, stripeProperties.getWebhookSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Stripe-Signature", signature);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/webhooks/stripe", HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Optional<TenantRecord> updatedTenant = tenantRegistryService.findByTenantId(tenant.tenantId());
        assertTrue(updatedTenant.isPresent());
        assertEquals(stripeCustomerId, updatedTenant.get().stripeCustomerId());

        TenantContext.setTenant(tenant.schemaName());
        try {
            assertTrue(customerRepository.findByStripeCustomerId(stripeCustomerId).isPresent());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void wrongSignatureIsRejected() {
        String payload = """
                {"id": "evt_test_bad", "object": "event", "api_version": "2020-08-27", "type": "checkout.session.completed", "data": {"object": {}}}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Stripe-Signature", "t=1,v1=not-a-real-signature");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/webhooks/stripe", HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}