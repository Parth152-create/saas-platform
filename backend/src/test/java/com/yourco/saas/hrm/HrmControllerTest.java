package com.yourco.saas.hrm;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.hrm.dto.*;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class HrmControllerTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String loginAs(TenantRecord tenant, Role role) {
        String email = role.name().toLowerCase() + "@hrm.test";
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), email, "password123"), TokenResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        return loginResponse.getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void listEmployeesReturnsSeededEmployees() {
        TenantRecord tenant = provisionTenant("hrm-list");
        String token = loginAs(tenant, Role.USER);

        ResponseEntity<EmployeeDto[]> response = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                EmployeeDto[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length >= 6, "Should include seeded employees from V10 migration");
    }

    @Test
    void adminCanCreateAndGetEmployee() {
        TenantRecord tenant = provisionTenant("hrm-create");
        String adminToken = loginAs(tenant, Role.ADMIN);

        CreateEmployeeRequest req = new CreateEmployeeRequest(
                "EMP-TEST-999",
                "Jane Tester",
                "jane.tester@workspace.io",
                "Engineering",
                "Staff Engineer",
                EmployeeStatus.ACTIVE,
                LocalDate.now(),
                "+1 (555) 000-1111",
                "Remote",
                "Austin, TX",
                "Alexander Chen",
                "bg-zinc-800 text-zinc-100",
                new BigDecimal("99.50"),
                160
        );

        ResponseEntity<EmployeeDto> createResponse = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(adminToken)),
                EmployeeDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertEquals("EMP-TEST-999", createResponse.getBody().employeeId());
        assertEquals("Jane Tester", createResponse.getBody().name());

        // Now fetch by ID
        ResponseEntity<EmployeeDto> getResponse = restTemplate.exchange(
                "/api/hrm/employees/" + createResponse.getBody().id(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                EmployeeDto.class
        );
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("EMP-TEST-999", getResponse.getBody().employeeId());
    }

    @Test
    void nonAdminCannotCreateEmployee() {
        TenantRecord tenant = provisionTenant("hrm-rbac");
        String userToken = loginAs(tenant, Role.USER);

        CreateEmployeeRequest req = new CreateEmployeeRequest(
                "EMP-FAIL-1",
                "Unauthorized User",
                "unauthorized@workspace.io",
                "Engineering",
                "Tester",
                EmployeeStatus.ACTIVE,
                LocalDate.now(),
                null,
                "Hybrid",
                null,
                null,
                null,
                null,
                null
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(userToken)),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void listDepartmentsAndStatsWork() {
        TenantRecord tenant = provisionTenant("hrm-depts");
        String token = loginAs(tenant, Role.USER);

        ResponseEntity<DepartmentDto[]> deptsResponse = restTemplate.exchange(
                "/api/hrm/departments",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                DepartmentDto[].class
        );
        assertEquals(HttpStatus.OK, deptsResponse.getStatusCode());
        assertNotNull(deptsResponse.getBody());
        assertTrue(deptsResponse.getBody().length >= 5);

        ResponseEntity<HrmStatsDto> statsResponse = restTemplate.exchange(
                "/api/hrm/stats",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                HrmStatsDto.class
        );
        assertEquals(HttpStatus.OK, statsResponse.getStatusCode());
        assertNotNull(statsResponse.getBody());
        assertTrue(statsResponse.getBody().totalEmployees() >= 6);
        assertTrue(statsResponse.getBody().totalDepartments() >= 5);
    }
}
