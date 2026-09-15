package com.yourco.saas.tenant;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TenantOnboardingService {

    private final TenantProvisioningService tenantProvisioningService;
    private final TenantRegistryService tenantRegistryService;

    public TenantOnboardingService(TenantProvisioningService tenantProvisioningService,
                                    TenantRegistryService tenantRegistryService) {
        this.tenantProvisioningService = tenantProvisioningService;
        this.tenantRegistryService = tenantRegistryService;
    }

    public Optional<TenantRecord> onboardTenant(String tenantId, String plan) {
        if (tenantRegistryService.findByTenantId(tenantId).isPresent()) {
            return Optional.empty();
        }
        String schemaName = "tenant_" + tenantId.replace('-', '_');
        return Optional.of(tenantProvisioningService.provisionTenant(tenantId, schemaName, plan));
    }
}