package com.yourco.saas.tenant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Optional;

@Repository
public class TenantRegistryService {

    private final JdbcTemplate jdbcTemplate;

    public TenantRegistryService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Optional<TenantRecord> findByTenantId(String tenantId) {
        String sql = """
                SELECT id, tenant_id, schema_name, status, plan
                FROM public.tenant_registry
                WHERE tenant_id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TenantRecord(
                rs.getLong("id"),
                rs.getString("tenant_id"),
                rs.getString("schema_name"),
                rs.getString("status"),
                rs.getString("plan")
        ), tenantId).stream().findFirst();
    }

    public TenantRecord register(String tenantId, String schemaName, String plan) {
        String sql = """
                INSERT INTO public.tenant_registry (tenant_id, schema_name, status, plan)
                VALUES (?, ?, 'ACTIVE', ?)
                RETURNING id, tenant_id, schema_name, status, plan
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new TenantRecord(
                rs.getLong("id"),
                rs.getString("tenant_id"),
                rs.getString("schema_name"),
                rs.getString("status"),
                rs.getString("plan")
        ), tenantId, schemaName, plan);
    }
}