package com.yourco.saas;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BackendApplicationDotEnvTest {

    @Test
    void findDotEnvLocatesRootDotEnvFile() {
        Path dotEnvPath = BackendApplication.findDotEnv();
        assertNotNull(dotEnvPath, "Should locate .env file in root repository");
        assertTrue(dotEnvPath.toFile().exists(), ".env file must exist on disk");
        assertEquals(".env", dotEnvPath.getFileName().toString());
    }

    @Test
    void loadDotEnvPopulatesSystemPropertiesWithoutOverridingLocalhostSettings() {
        BackendApplication.loadDotEnv();

        String stripeSecret = System.getProperty("STRIPE_SECRET_KEY");
        assertNotNull(stripeSecret, "STRIPE_SECRET_KEY should be loaded into System properties");
        assertFalse(stripeSecret.isBlank(), "STRIPE_SECRET_KEY should not be blank");

        String proPrice = System.getProperty("STRIPE_PRICE_PRO_ID");
        assertNotNull(proPrice, "STRIPE_PRICE_PRO_ID should be loaded into System properties");
        assertTrue(proPrice.startsWith("price_"), "STRIPE_PRICE_PRO_ID should start with price_");
        assertFalse(proPrice.startsWith("price_price_"), "STRIPE_PRICE_PRO_ID must not have duplicate price_ prefix");

        String entPrice = System.getProperty("STRIPE_PRICE_ENTERPRISE_ID");
        assertNotNull(entPrice, "STRIPE_PRICE_ENTERPRISE_ID should be loaded into System properties");
        assertTrue(entPrice.startsWith("price_"), "STRIPE_PRICE_ENTERPRISE_ID should start with price_");

        String checkoutSuccessUrl = System.getProperty("STRIPE_CHECKOUT_SUCCESS_URL");
        assertNotNull(checkoutSuccessUrl, "STRIPE_CHECKOUT_SUCCESS_URL should be loaded");
        assertTrue(checkoutSuccessUrl.contains("localhost:5173"), "STRIPE_CHECKOUT_SUCCESS_URL must point to Vite port 5173");

        // Ensure container-only postgres host wasn't set in system properties
        String dsUrl = System.getProperty("SPRING_DATASOURCE_URL");
        if (dsUrl != null) {
            assertFalse(dsUrl.contains("postgres:5432"), "SPRING_DATASOURCE_URL should not point to container postgres:5432 on localhost");
        }
    }
}
