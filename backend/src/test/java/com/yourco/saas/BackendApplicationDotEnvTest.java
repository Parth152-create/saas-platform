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
    void loadDotEnvLoadsConfigurationWithoutOverridingExistingEnvironment() {
        BackendApplication.loadDotEnv();

        String stripeSecret = propertyOrEnvironment("STRIPE_SECRET_KEY");
        assertNotNull(stripeSecret, "STRIPE_SECRET_KEY should be available");
        assertFalse(stripeSecret.isBlank(), "STRIPE_SECRET_KEY should not be blank");

        String proPrice = propertyOrEnvironment("STRIPE_PRICE_PRO_ID");
        assertNotNull(proPrice, "STRIPE_PRICE_PRO_ID should be available");
        assertTrue(proPrice.startsWith("price_"),
                "STRIPE_PRICE_PRO_ID should start with price_");
        assertFalse(proPrice.startsWith("price_price_"),
                "STRIPE_PRICE_PRO_ID must not have duplicate price_ prefix");

        String entPrice = propertyOrEnvironment("STRIPE_PRICE_ENTERPRISE_ID");
        assertNotNull(entPrice, "STRIPE_PRICE_ENTERPRISE_ID should be available");
        assertTrue(entPrice.startsWith("price_"),
                "STRIPE_PRICE_ENTERPRISE_ID should start with price_");

        String checkoutSuccessUrl = propertyOrEnvironment("STRIPE_CHECKOUT_SUCCESS_URL");
        assertNotNull(checkoutSuccessUrl,
                "STRIPE_CHECKOUT_SUCCESS_URL should be available");
        assertTrue(checkoutSuccessUrl.contains("localhost:5173"),
                "STRIPE_CHECKOUT_SUCCESS_URL must point to Vite port 5173");

        // Ensure container-only postgres host wasn't set in system properties
        String dsUrl = System.getProperty("SPRING_DATASOURCE_URL");
        if (dsUrl != null) {
            assertFalse(
                    dsUrl.contains("postgres:5432"),
                    "SPRING_DATASOURCE_URL should not point to container postgres:5432 on localhost"
            );
        }
    }

    private static String propertyOrEnvironment(String key) {
        String value = System.getProperty(key);
        return value != null ? value : System.getenv(key);
    }
}