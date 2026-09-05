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
    private final CustomerRepository customerRepository;

    public TenantDebugController(TenantProvisioningService provisioningService,
                                  CustomerRepository customerRepository) {
        this.provisioningService = provisioningService;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/{schema}/provision")
    public Map<String, String> provision(@PathVariable String schema) {
        provisioningService.provisionTenant(schema);
        return Map.of("status", "provisioned", "schema", schema);
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