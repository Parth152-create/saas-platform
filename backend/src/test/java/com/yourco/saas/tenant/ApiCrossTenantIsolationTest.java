package com.yourco.saas.tenant;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.hrm.Department;
import com.yourco.saas.domain.hrm.DepartmentRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.hrm.dto.CreateEmployeeRequest;
import com.yourco.saas.hrm.dto.EmployeeDto;
import com.yourco.saas.hrm.dto.UpdateEmployeeRequest;
import com.yourco.saas.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestRestTemplate
class ApiCrossTenantIsolationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtEncoder jwtEncoder;

    private String loginAsAdmin(TenantRecord tenant, String email) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, Role.ADMIN);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> res = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), email, RAW_PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        return res.getBody().accessToken();
    }

    private Employee createEmployeeInTenant(TenantRecord tenant, String employeeId, String name, String email) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            Employee emp = new Employee();
            emp.setEmployeeId(employeeId);
            emp.setName(name);
            emp.setEmail(email);
            emp.setDepartment("Engineering");
            emp.setPosition("Engineer");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setHireDate(LocalDate.now());
            emp.setAttendanceRate(new BigDecimal("100.00"));
            emp.setBillableHours(40);
            return employeeRepository.save(emp);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenantACannotAccessTenantBEmployeeById() {
        TenantRecord tenantA = provisionTenant("api-iso-a1");
        TenantRecord tenantB = provisionTenant("api-iso-b1");

        String tokenA = loginAsAdmin(tenantA, "admin@tenant-a1.test");
        Employee empB = createEmployeeInTenant(tenantB, "EMP-B-01", "Tenant B Employee", "emp-b1@workspace.test");

        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);

        // GET /api/hrm/employees/{empB.id} with Tenant A token -> 404 Not Found
        ResponseEntity<String> getRes = restTemplate.exchange(
                "/api/hrm/employees/" + empB.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headersA),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getRes.getStatusCode(),
                "Tenant A user must receive 404 when querying Tenant B employee ID");

        // PUT /api/hrm/employees/{empB.id} with Tenant A token -> 404 Not Found
        UpdateEmployeeRequest updateReq = new UpdateEmployeeRequest(
                "Attacked Name", null, null, null, null, null, null, null, null, null, null, null, null);
        ResponseEntity<String> putRes = restTemplate.exchange(
                "/api/hrm/employees/" + empB.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, headersA),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, putRes.getStatusCode(),
                "Tenant A user must receive 404 when attempting to update Tenant B employee ID");

        // DELETE /api/hrm/employees/{empB.id} with Tenant A token -> 404 Not Found
        ResponseEntity<String> delRes = restTemplate.exchange(
                "/api/hrm/employees/" + empB.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(headersA),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, delRes.getStatusCode(),
                "Tenant A user must receive 404 when attempting to delete Tenant B employee ID");

        // Verify empB was NOT modified or deleted in Tenant B
        TenantContext.setTenant(tenantB.schemaName());
        try {
            Employee stillExists = employeeRepository.findById(empB.getId()).orElseThrow();
            assertEquals("Tenant B Employee", stillExists.getName());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void employeeListOnlyReturnsCurrentTenantData() {
        TenantRecord tenantA = provisionTenant("api-iso-a2");
        TenantRecord tenantB = provisionTenant("api-iso-b2");

        String tokenA = loginAsAdmin(tenantA, "admin@tenant-a2.test");
        createEmployeeInTenant(tenantA, "EMP-A-01", "Alice Tenant A", "alice@tenant-a2.test");
        createEmployeeInTenant(tenantB, "EMP-B-02", "Bob Tenant B", "bob@tenant-b2.test");

        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);

        ResponseEntity<List<EmployeeDto>> listRes = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.GET,
                new HttpEntity<>(headersA),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, listRes.getStatusCode());
        List<EmployeeDto> list = listRes.getBody();
        assertNotNull(list);

        // Must contain Alice and NOT contain Bob
        boolean hasAlice = list.stream().anyMatch(e -> "EMP-A-01".equals(e.employeeId()));
        boolean hasBob = list.stream().anyMatch(e -> "EMP-B-02".equals(e.employeeId()));

        assertTrue(hasAlice, "Tenant A list must contain Tenant A employee");
        assertFalse(hasBob, "Tenant A list must NEVER contain Tenant B employee");
    }

    @Test
    void headerSpoofingDoesNotOverrideJwtTenant() {
        TenantRecord tenantA = provisionTenant("api-iso-a3");
        TenantRecord tenantB = provisionTenant("api-iso-b3");

        String tokenA = loginAsAdmin(tenantA, "admin@tenant-a3.test");
        Employee empB = createEmployeeInTenant(tenantB, "EMP-B-03", "Bob B3", "bob@b3.test");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        headers.set("X-Tenant-ID", tenantB.tenantId());
        headers.set("Tenant-Id", tenantB.tenantId());
        headers.set("schema_name", tenantB.schemaName());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/hrm/employees/" + empB.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Header spoofing attempts must be ignored; tenant is strictly derived from JWT");
    }

    @Test
    void tokenWithNonExistentTenantIsRejectedWith401() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("saas-platform")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .id(UUID.randomUUID().toString())
                .claim("tenant_id", "non-existent-tenant-xyz")
                .claim("role", "ADMIN")
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String forgedToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(forgedToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "A JWT with an unknown tenant_id must be rejected with 401 Unauthorized");
    }
}
