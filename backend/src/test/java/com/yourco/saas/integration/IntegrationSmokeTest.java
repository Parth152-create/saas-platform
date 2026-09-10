package com.yourco.saas.integration;

import com.yourco.saas.tenant.TenantProvisioningService;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Testcontainers base class itself: both containers come up,
 * the @ServiceConnection-backed DataSource/Redis beans work, the GLOBAL
 * Flyway migration ran automatically on context startup against the
 * ephemeral Postgres container, and TenantProvisioningService can
 * provision + migrate a brand-new TENANT schema against that same
 * container. Ordered explicitly so containers-up runs before anything
 * that depends on them.
 * <p>
 * Note: globalFlywayMigrationRanAutomaticallyOnStartup only asserts the
 * registry table EXISTS, not that it's empty. public.tenant_registry is
 * shared state across every test class using this base class - other
 * tests (e.g. CrossTenantIsolationTest) may run first in the same
 * container and leave rows in it. Asserting "empty" here would make this
 * test's pass/fail depend on Surefire's class execution order, which
 * isn't guaranteed.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationSmokeTest extends IntegrationTestBase {

    @Autowired
    DataSource dataSource;

    @Autowired
    TenantRegistryService tenantRegistryService;

    @Autowired
    TenantProvisioningService tenantProvisioningService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    @Order(1)
    void containersAreUpAndSpringContextLoaded() {
        assertTrue(POSTGRES.isRunning());
        assertTrue(REDIS.isRunning());
    }

    @Test
    @Order(2)
    void globalFlywayMigrationRanAutomaticallyOnStartup() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM public.tenant_registry", Integer.class);
        assertNotNull(count,
                "public.tenant_registry should exist and be queryable once the global Flyway migration has run");
    }

    @Test
    @Order(3)
    void tenantSchemaProvisionsAndMigratesEndToEnd() {
        TenantRecord record = tenantRegistryService.register(
                "smoke-test-tenant", "tenant_smoke_test", "FREE");
        tenantProvisioningService.provisionTenant(record.schemaName());

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer userCount = jdbc.queryForObject(
                "SELECT count(*) FROM tenant_smoke_test.users", Integer.class);
        Integer auditCount = jdbc.queryForObject(
                "SELECT count(*) FROM tenant_smoke_test.audit_log", Integer.class);

        assertEquals(0, userCount);
        assertEquals(0, auditCount);
    }

    @Test
    @Order(4)
    void redisIsReachableViaServiceConnection() {
        redisTemplate.opsForValue().set("smoke-test-key", "ok");
        assertEquals("ok", redisTemplate.opsForValue().get("smoke-test-key"));
    }
}