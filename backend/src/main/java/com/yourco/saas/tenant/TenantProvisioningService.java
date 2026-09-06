package com.yourco.saas.tenant;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class TenantProvisioningService {

    private final DataSource dataSource;

    public TenantProvisioningService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void provisionTenant(String schemaName) {
        validateSchemaName(schemaName);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .load()
                .migrate();
    }

    public void provisionTenant(String tenantId, String schemaName, String plan) {
        validateTenantId(tenantId);
        validateSchemaName(schemaName);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .load()
                .migrate();
    }

    private void validateTenantId(String tenantId) {
        if (!tenantId.matches("^[a-z][a-z0-9_-]{0,49}$")) {
            throw new IllegalArgumentException("Invalid tenant ID: " + tenantId);
        }
    }

    private void validateSchemaName(String schemaName) {
        if (!schemaName.matches("^[a-z][a-z0-9_]{0,62}$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }
}