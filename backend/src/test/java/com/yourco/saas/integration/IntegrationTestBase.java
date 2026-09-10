package com.yourco.saas.integration;

import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantProvisioningService;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("saas_db")
                    .withUsername("saas")
                    .withPassword("saas_dev_pw");

    @ServiceConnection
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @Autowired
    protected TenantRegistryService tenantRegistryService;

    @Autowired
    protected TenantProvisioningService tenantProvisioningService;

    @Autowired
    protected DataSource dataSource;

    private final List<String> provisionedSchemas = new ArrayList<>();

    @BeforeEach
    protected void clearTenantContextBeforeEach() {
        TenantContext.clear();
    }

    /**
     * Provisions a fresh, uniquely-named tenant (registry row + migrated
     * schema) for use within a single test method. `label` can contain
     * hyphens (e.g. "login-ok") for readability - schema names can't
     * (TenantProvisioningService enforces ^[a-z][a-z0-9_]{0,62}$), so
     * hyphens are normalized to underscores for the schema name only.
     * tenantId keeps the original label as-is since hyphens are valid there.
     */
    protected TenantRecord provisionTenant(String label) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String schemaSafeLabel = label.toLowerCase().replace("-", "_");
        String tenantId = "it-" + label + "-" + suffix;
        String schemaName = "tenant_it_" + schemaSafeLabel + "_" + suffix;
        TenantRecord record = tenantRegistryService.register(tenantId, schemaName, "FREE");
        tenantProvisioningService.provisionTenant(record.schemaName());
        provisionedSchemas.add(record.schemaName());
        return record;
    }

    @AfterEach
    protected void cleanUpProvisionedTenants() {
        TenantContext.clear();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String schema : provisionedSchemas) {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            jdbc.update("DELETE FROM public.tenant_registry WHERE schema_name = ?", schema);
        }
        provisionedSchemas.clear();
    }
}