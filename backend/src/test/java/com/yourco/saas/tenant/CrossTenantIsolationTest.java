package com.yourco.saas.tenant;

import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flagship test for the whole multi-tenancy architecture: proves Hibernate's
 * schema-per-tenant routing (TenantIdentifierResolver + TenantConnectionProvider,
 * driven by TenantContext) actually isolates data at the JPA layer - not just
 * that Flyway can create schemas, which IntegrationSmokeTest already covers.
 * <p>
 * Deliberately NOT @Transactional. Wrapping the test method in one Spring-
 * managed transaction would open a single Hibernate Session up front, which
 * resolves the tenant identifier ONCE and caches it for that Session's
 * lifetime. Switching TenantContext mid-test would then do nothing - the
 * whole test would silently run against whichever tenant was active at the
 * very first repository call. Each repository call here needs to open its
 * OWN Session, after TenantContext has been updated - which only happens
 * with the default (non-transactional) test setup, since Spring Data JPA
 * repository methods manage their own short transaction per call.
 */
class CrossTenantIsolationTest extends IntegrationTestBase {

    @Autowired
    UserRepository userRepository;

    @Test
    void userCreatedInTenantAIsInvisibleFromTenantB() {
        String schemaA = provisionTenant("a").schemaName();
        String schemaB = provisionTenant("b").schemaName();

        TenantContext.setTenant(schemaA);
        userRepository.save(UserTestFactory.localUser("isolation-test@tenant-a.test", Role.USER));

        TenantContext.setTenant(schemaB);
        Optional<User> foundFromB = userRepository.findByEmailIgnoreCase("isolation-test@tenant-a.test");
        assertTrue(foundFromB.isEmpty(),
                "Tenant A's user leaked into tenant B's query results");

        TenantContext.setTenant(schemaA);
        Optional<User> foundFromA = userRepository.findByEmailIgnoreCase("isolation-test@tenant-a.test");
        assertTrue(foundFromA.isPresent(),
                "Tenant A's own user should still be visible from tenant A");
    }

    @Test
    void findAllOnlyReturnsCurrentTenantsRows() {
        String schemaA = provisionTenant("a").schemaName();
        String schemaB = provisionTenant("b").schemaName();

        TenantContext.setTenant(schemaA);
        userRepository.save(UserTestFactory.localUser("alice@tenant-a.test", Role.USER));
        userRepository.save(UserTestFactory.localUser("bob@tenant-a.test", Role.USER));

        TenantContext.setTenant(schemaB);
        userRepository.save(UserTestFactory.localUser("carol@tenant-b.test", Role.USER));

        List<User> tenantBUsers = userRepository.findAll();
        assertEquals(1, tenantBUsers.size(),
                "Tenant B should only see its own single row, not tenant A's two rows");
        assertEquals("carol@tenant-b.test", tenantBUsers.get(0).getEmail());

        TenantContext.setTenant(schemaA);
        List<User> tenantAUsers = userRepository.findAll();
        assertEquals(2, tenantAUsers.size(),
                "Tenant A should see exactly its own two rows");
    }

    @Test
    void sameEmailCanExistIndependentlyInTwoTenants() {
        String schemaA = provisionTenant("a").schemaName();
        String schemaB = provisionTenant("b").schemaName();

        // uq_users_email is a unique index PER SCHEMA - only safe because
        // each tenant is a physically separate schema. If isolation were
        // broken, the second save would hit a unique constraint violation
        // instead of succeeding.
        TenantContext.setTenant(schemaA);
        userRepository.save(UserTestFactory.localUser("shared@example.test", Role.USER));

        TenantContext.setTenant(schemaB);
        userRepository.save(UserTestFactory.localUser("shared@example.test", Role.USER));

        TenantContext.setTenant(schemaA);
        assertTrue(userRepository.findByEmailIgnoreCase("shared@example.test").isPresent());

        TenantContext.setTenant(schemaB);
        assertTrue(userRepository.findByEmailIgnoreCase("shared@example.test").isPresent());
    }

    @Test
    void missingTenantContextFailsInsteadOfSilentlyLeakingData() {
        // No TenantContext set -> resolves to TenantIdentifierResolver.DEFAULT_TENANT
        // ("public"), which has no users table at all (only tenant_registry
        // lives in public). Should fail loudly, not fall through to some
        // other schema's data.
        assertThrows(InvalidDataAccessResourceUsageException.class,
                userRepository::findAll,
                "Querying with no tenant context set should fail, not silently return another tenant's rows");
    }
}