package com.yourco.saas.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.yourco.saas.billing.dto.BillingSummaryResponse;
import com.yourco.saas.billing.dto.CreateCheckoutSessionRequest;
import com.yourco.saas.billing.dto.CreateCheckoutSessionResponse;
import com.yourco.saas.billing.dto.CreatePortalSessionResponse;
import com.yourco.saas.billing.dto.FeatureEntitlementsResponse;
import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final StripeProperties stripeProperties;
    private final TenantRegistryService tenantRegistryService;
    private final UserRepository userRepository;
    private final StripeCheckoutSessionCreator checkoutSessionCreator;
    private final BillingService billingService;
    private final FeatureEntitlementService featureEntitlementService;

    public BillingController(StripeProperties stripeProperties,
                              TenantRegistryService tenantRegistryService,
                              UserRepository userRepository,
                              StripeCheckoutSessionCreator checkoutSessionCreator,
                              BillingService billingService,
                              FeatureEntitlementService featureEntitlementService) {
        this.stripeProperties = stripeProperties;
        this.tenantRegistryService = tenantRegistryService;
        this.userRepository = userRepository;
        this.checkoutSessionCreator = checkoutSessionCreator;
        this.billingService = billingService;
        this.featureEntitlementService = featureEntitlementService;
    }

    @PostMapping("/checkout-session")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateCheckoutSessionResponse> createCheckoutSession(@RequestBody @Valid CreateCheckoutSessionRequest request) {
        if (request.planTier() == PlanTier.FREE || request.planTier() == PlanTier.STARTER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create a checkout session for the FREE/STARTER tier");
        }

        TenantRecord tenant = tenantRegistryService.findBySchemaName(TenantContext.getTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant context not resolved"));

        String priceId = stripeProperties.getPriceIdForTier(request.planTier())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "No Stripe price configured for tier " + request.planTier()));

        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(stripeProperties.getCheckoutSuccessUrl())
                .setCancelUrl(stripeProperties.getCheckoutCancelUrl())
                .setClientReferenceId(tenant.tenantId())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build());

        if (tenant.stripeCustomerId() != null) {
            paramsBuilder.setCustomer(tenant.stripeCustomerId());
        } else {
            paramsBuilder.setCustomerEmail(user.getEmail());
        }

        try {
            Session session = checkoutSessionCreator.create(paramsBuilder.build());
            return ResponseEntity.ok(new CreateCheckoutSessionResponse(session.getUrl()));
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe checkout session creation failed: " + e.getMessage());
        }
    }

    @PostMapping("/portal-session")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreatePortalSessionResponse> createPortalSession() {
        return ResponseEntity.ok(billingService.createPortalSession());
    }

    @GetMapping({"", "/current"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BillingSummaryResponse> getBillingSummary() {
        return ResponseEntity.ok(billingService.getBillingSummary());
    }

    @GetMapping("/entitlements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeatureEntitlementsResponse> getEntitlements() {
        return ResponseEntity.ok(featureEntitlementService.getEntitlementsForCurrentTenant());
    }
}