package com.yourco.saas.tenant;

import com.yourco.saas.domain.customer.Customer;
import com.yourco.saas.domain.customer.CustomerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/tenants")
public class TenantDebugController {

    private final TenantProvisioningService provisioningService;
    private final TenantRegistryService tenantRegistryService;
    private final CustomerRepository customerRepository;

    public TenantDebugController(TenantProvisioningService provisioningService,
                                  TenantRegistryService tenantRegistryService,
                                  CustomerRepository customerRepository) {
        this.provisioningService = provisioningService;
        this.tenantRegistryService = tenantRegistryService;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/{schema}/provision")
    public Map<String, String> provision(@PathVariable String schema) {
        provisioningService.provisionTenant(schema);
        return Map.of("status", "provisioned", "schema", schema);
    }

    @PostMapping("/{tenantId}/provision-registered")
    public Map<String, String> provisionRegistered(
            @PathVariable String tenantId,
            @RequestParam String schema,
            @RequestParam(defaultValue = "FREE") String plan) {
        provisioningService.provisionTenant(tenantId, schema, plan);
        TenantRecord tenant = tenantRegistryService.register(tenantId, schema, plan);
        return Map.of(
                "status", "provisioned",
                "tenantId", tenant.tenantId(),
                "schema", tenant.schemaName());
    }

    @PostMapping("/{schema}/customers")
    public Customer createCustomer(@PathVariable String schema, @RequestBody Customer customer) {
        TenantContext.setTenant(schema);
        try {
            return customerRepository.save(customer);
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/{schema}/customers")
    public List<Customer> listCustomers(@PathVariable String schema) {
        TenantContext.setTenant(schema);
        try {
            return customerRepository.findAll();
        } finally {
            TenantContext.clear();
        }
    }
}