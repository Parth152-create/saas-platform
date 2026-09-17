package com.yourco.saas.billing;

import com.yourco.saas.domain.billing.PlanTier;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {
    private String secretKey;
    private String webhookSecret;
    private Map<String, String> priceTiers = Map.of();
    private String checkoutSuccessUrl;
    private String checkoutCancelUrl;
    private String portalReturnUrl = "http://localhost:5173/billing";

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public Map<String, String> getPriceTiers() { return priceTiers; }
    public void setPriceTiers(Map<String, String> priceTiers) { this.priceTiers = priceTiers; }
    public String getCheckoutSuccessUrl() { return checkoutSuccessUrl; }
    public void setCheckoutSuccessUrl(String checkoutSuccessUrl) { this.checkoutSuccessUrl = checkoutSuccessUrl; }
    public String getCheckoutCancelUrl() { return checkoutCancelUrl; }
    public void setCheckoutCancelUrl(String checkoutCancelUrl) { this.checkoutCancelUrl = checkoutCancelUrl; }
    public String getPortalReturnUrl() { return portalReturnUrl; }
    public void setPortalReturnUrl(String portalReturnUrl) { this.portalReturnUrl = portalReturnUrl; }

    public Optional<String> getPriceIdForTier(PlanTier tier) {
        return priceTiers.entrySet().stream()
                .filter(e -> e.getValue().equals(tier.name()))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}