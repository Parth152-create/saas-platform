package com.yourco.saas.billing;

import com.yourco.saas.domain.billing.PlanTier;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {
    private String secretKey;
    private String webhookSecret;
    private PriceTiers priceTiers = new PriceTiers();
    private String checkoutSuccessUrl;
    private String checkoutCancelUrl;
    private String portalReturnUrl = "http://localhost:5173/billing";

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public PriceTiers getPriceTiers() {
        return priceTiers;
    }

    public void setPriceTiers(PriceTiers priceTiers) {
        this.priceTiers = priceTiers;
    }

    public String getCheckoutSuccessUrl() { return checkoutSuccessUrl; }
    public void setCheckoutSuccessUrl(String checkoutSuccessUrl) { this.checkoutSuccessUrl = checkoutSuccessUrl; }

    public String getCheckoutCancelUrl() { return checkoutCancelUrl; }
    public void setCheckoutCancelUrl(String checkoutCancelUrl) { this.checkoutCancelUrl = checkoutCancelUrl; }

    public String getPortalReturnUrl() { return portalReturnUrl; }
    public void setPortalReturnUrl(String portalReturnUrl) { this.portalReturnUrl = portalReturnUrl; }

    public Optional<String> getPriceIdForTier(PlanTier tier) {
        if (tier == null || priceTiers == null) {
            return Optional.empty();
        }
        return switch (tier) {
            case PRO -> Optional.ofNullable(priceTiers.getProPriceId())
                    .filter(id -> !id.isBlank());

            case ENTERPRISE -> Optional.ofNullable(priceTiers.getEnterprisePriceId())
                    .filter(id -> !id.isBlank());

            default -> Optional.empty();
        };
    }

    public Optional<PlanTier> getTierForPriceId(String priceId) {
        if (priceId == null || priceId.isBlank() || priceTiers == null) {
            return Optional.empty();
        }

        if (priceId.equals(priceTiers.getProPriceId())) {
            return Optional.of(PlanTier.PRO);
        }

        if (priceId.equals(priceTiers.getEnterprisePriceId())) {
            return Optional.of(PlanTier.ENTERPRISE);
        }

        return Optional.empty();
    }

    public static class PriceTiers {

        private String proPriceId;
        private String enterprisePriceId;

        public String getProPriceId() {
            return proPriceId;
        }

        public void setProPriceId(String proPriceId) {
            this.proPriceId = proPriceId;
        }

        public String getEnterprisePriceId() {
            return enterprisePriceId;
        }

        public void setEnterprisePriceId(String enterprisePriceId) {
            this.enterprisePriceId = enterprisePriceId;
        }
    }
}