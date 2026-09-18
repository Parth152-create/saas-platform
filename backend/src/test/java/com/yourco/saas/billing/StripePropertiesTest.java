package com.yourco.saas.billing;

import com.yourco.saas.domain.billing.PlanTier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StripePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(StripeConfig.class));

    @Test
    void resolvesPriceIdForTiersCorrectly() {
        StripeProperties properties = new StripeProperties();
        properties.getPriceTiers().setProPriceId("price_pro_test_123");
        properties.getPriceTiers().setEnterprisePriceId("price_ent_test_456");

        assertEquals(Optional.of("price_pro_test_123"), properties.getPriceIdForTier(PlanTier.PRO));
        assertEquals(Optional.of("price_ent_test_456"), properties.getPriceIdForTier(PlanTier.ENTERPRISE));
        assertEquals(Optional.empty(), properties.getPriceIdForTier(PlanTier.FREE));
        assertEquals(Optional.empty(), properties.getPriceIdForTier(PlanTier.STARTER));
        assertEquals(Optional.empty(), properties.getPriceIdForTier(null));
    }

    @Test
    void resolvesTierForPriceIdCorrectly() {
        StripeProperties properties = new StripeProperties();
        properties.getPriceTiers().setProPriceId("price_pro_test_123");
        properties.getPriceTiers().setEnterprisePriceId("price_ent_test_456");

        assertEquals(Optional.of(PlanTier.PRO), properties.getTierForPriceId("price_pro_test_123"));
        assertEquals(Optional.of(PlanTier.ENTERPRISE), properties.getTierForPriceId("price_ent_test_456"));
        assertEquals(Optional.empty(), properties.getTierForPriceId("price_unknown_999"));
        assertEquals(Optional.empty(), properties.getTierForPriceId(""));
        assertEquals(Optional.empty(), properties.getTierForPriceId(null));
    }

    @Test
    void bindsNestedPropertiesFromApplicationPropertiesCorrectly() {
        contextRunner
                .withPropertyValues(
                        "app.stripe.secret-key=sk_test_mock_123",
                        "app.stripe.webhook-secret=whsec_mock_456",
                        "app.stripe.price-tiers.pro-price-id=price_test_pro_123",
                        "app.stripe.price-tiers.enterprise-price-id=price_test_enterprise_456"
                )
                .run(context -> {
                    assertNotNull(context.getBean(StripeProperties.class));
                    StripeProperties properties = context.getBean(StripeProperties.class);

                    assertEquals("sk_test_mock_123", properties.getSecretKey());
                    assertEquals("whsec_mock_456", properties.getWebhookSecret());

                    assertEquals(Optional.of("price_test_pro_123"), properties.getPriceIdForTier(PlanTier.PRO));
                    assertEquals(Optional.of("price_test_enterprise_456"), properties.getPriceIdForTier(PlanTier.ENTERPRISE));

                    assertEquals(Optional.of(PlanTier.PRO), properties.getTierForPriceId("price_test_pro_123"));
                    assertEquals(Optional.of(PlanTier.ENTERPRISE), properties.getTierForPriceId("price_test_enterprise_456"));
                });
    }

    @Test
    void handlesBlankPriceIdsGracefully() {
        StripeProperties properties = new StripeProperties();
        properties.getPriceTiers().setProPriceId("   ");
        properties.getPriceTiers().setEnterprisePriceId("");

        assertEquals(Optional.empty(), properties.getPriceIdForTier(PlanTier.PRO));
        assertEquals(Optional.empty(), properties.getPriceIdForTier(PlanTier.ENTERPRISE));
    }
}
