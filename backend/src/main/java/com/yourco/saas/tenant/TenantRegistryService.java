package com.yourco.saas.tenant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class TenantRegistryService {

    private final JdbcTemplate jdbcTemplate;

    public TenantRegistryService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Optional<TenantRecord> findByTenantId(String tenantId) {
        String sql = """
                SELECT id, tenant_id, schema_name, status, plan, stripe_customer_id
                FROM public.tenant_registry
                WHERE tenant_id = ?
                """;
        return jdbcTemplate.query(sql, TenantRegistryService::mapRow, tenantId).stream().findFirst();
    }

    public Optional<TenantRecord> findBySchemaName(String schemaName) {
        String sql = """
                SELECT id, tenant_id, schema_name, status, plan, stripe_customer_id
                FROM public.tenant_registry
                WHERE schema_name = ?
                """;
        return jdbcTemplate.query(sql, TenantRegistryService::mapRow, schemaName).stream().findFirst();
    }

    public Optional<TenantRecord> findByStripeCustomerId(String stripeCustomerId) {
        String sql = """
                SELECT id, tenant_id, schema_name, status, plan, stripe_customer_id
                FROM public.tenant_registry
                WHERE stripe_customer_id = ?
                """;
        return jdbcTemplate.query(sql, TenantRegistryService::mapRow, stripeCustomerId).stream().findFirst();
    }

    public List<String> findAllSchemaNames() {
        return jdbcTemplate.queryForList(
                "SELECT schema_name FROM public.tenant_registry", String.class);
    }

    public TenantRecord register(String tenantId, String schemaName, String plan) {
        String sql = """
                INSERT INTO public.tenant_registry (tenant_id, schema_name, status, plan)
                VALUES (?, ?, 'ACTIVE', ?)
                RETURNING id, tenant_id, schema_name, status, plan, stripe_customer_id
                """;
        return jdbcTemplate.queryForObject(sql, TenantRegistryService::mapRow, tenantId, schemaName, plan);
    }

    public void updateStripeCustomerId(String tenantId, String stripeCustomerId) {
        jdbcTemplate.update(
                "UPDATE public.tenant_registry SET stripe_customer_id = ? WHERE tenant_id = ?",
                stripeCustomerId, tenantId);
    }

    public void updatePlan(String tenantId, String plan) {
        jdbcTemplate.update(
                "UPDATE public.tenant_registry SET plan = ? WHERE tenant_id = ?",
                plan, tenantId);
    }

    private static TenantRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TenantRecord(
                rs.getLong("id"),
                rs.getString("tenant_id"),
                rs.getString("schema_name"),
                rs.getString("status"),
                rs.getString("plan"),
                rs.getString("stripe_customer_id")
        );
    }
}