package com.yourco.saas.search;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.leave.LeaveRequest;
import com.yourco.saas.domain.leave.LeaveRequestRepository;
import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;
import com.yourco.saas.domain.projects.*;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.search.dto.SearchResultDto;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class GlobalSearchIntegrationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    LeaveRequestRepository leaveRequestRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createAndLoginUser(TenantRecord tenant, String email, Role role, String name) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            Employee emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6));
            emp.setName(name);
            emp.setEmail(email);
            emp.setDepartment("Engineering");
            emp.setPosition("Developer");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setHireDate(LocalDate.now());
            employeeRepository.save(emp);
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

    @Test
    @DisplayName("Global search finds employees, projects, tasks, departments and applies RBAC on leaves")
    void globalSearchMultientityAndRbac() {
        TenantRecord tenant = provisionTenant("search-suite");

        String user1Token = createAndLoginUser(tenant, "u1@search.test", Role.USER, "Arthur Dent");
        String user2Token = createAndLoginUser(tenant, "u2@search.test", Role.USER, "Ford Prefect");
        String mgrToken = createAndLoginUser(tenant, "mgr@search.test", Role.MANAGER, "Zaphod Beeblebrox");

        TenantContext.setTenant(tenant.schemaName());
        try {
            User u1 = userRepository.findByEmail("u1@search.test").orElseThrow();
            User u2 = userRepository.findByEmail("u2@search.test").orElseThrow();

            Project proj = new Project("Heart of Gold Navigation", "Spacetime drive system", "Sirius Corp",
                    ProjectStatus.ACTIVE, ProjectPriority.HIGH, LocalDate.now(), null, null, null);
            projectRepository.save(proj);

            Task task = new Task(proj, "Calibrate Infinite Improbability", "Details", TaskStatus.IN_PROGRESS,
                    TaskPriority.URGENT, u1.getId(), "Arthur Dent", u1.getEmail(), null, 8, 2, null, "Zaphod");
            taskRepository.save(task);

            LeaveRequest leave1 = new LeaveRequest(u1.getId(), null, "Arthur Dent", u1.getEmail(),
                    LeaveType.ANNUAL, LocalDate.now().plusDays(20), LocalDate.now().plusDays(25),
                    new BigDecimal("5.0"), "Trip to Magrathea", LeaveStatus.APPROVED);
            leaveRequestRepository.save(leave1);

            LeaveRequest leave2 = new LeaveRequest(u2.getId(), null, "Ford Prefect", u2.getEmail(),
                    LeaveType.SICK, LocalDate.now().plusDays(2), LocalDate.now().plusDays(3),
                    new BigDecimal("2.0"), "Betelgeuse flu", LeaveStatus.PENDING);
            leaveRequestRepository.save(leave2);
        } finally {
            TenantContext.clear();
        }

        // 1. Search for "Arthur" as u1 -> finds employee Arthur Dent and leave request
        ResponseEntity<List<SearchResultDto>> res1 = restTemplate.exchange(
                "/api/search?q=Arthur",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(user1Token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        List<SearchResultDto> results1 = res1.getBody();
        assertNotNull(results1);
        assertTrue(results1.stream().anyMatch(r -> "EMPLOYEE".equals(r.type()) && r.title().contains("Arthur")));

        // 2. Search for "Magrathea" (leave reason of Arthur) as Ford (u2) -> should return 0 (RBAC on leave requests!)
        ResponseEntity<List<SearchResultDto>> resFord = restTemplate.exchange(
                "/api/search?q=Magrathea",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(user2Token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, resFord.getStatusCode());
        assertTrue(resFord.getBody().isEmpty(), "User 2 cannot discover User 1's leave request in search");

        // 3. Search for "Magrathea" as Manager -> finds Arthur's leave request!
        ResponseEntity<List<SearchResultDto>> resMgr = restTemplate.exchange(
                "/api/search?q=Magrathea",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, resMgr.getStatusCode());
        assertEquals(1, resMgr.getBody().size());
        assertEquals("LEAVE", resMgr.getBody().get(0).type());

        // 4. Search for "Infinite" with type=TASK -> finds task
        ResponseEntity<List<SearchResultDto>> resTask = restTemplate.exchange(
                "/api/search?q=Infinite&type=TASK",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(user1Token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, resTask.getStatusCode());
        assertEquals(1, resTask.getBody().size());
        assertEquals("TASK", resTask.getBody().get(0).type());
        assertEquals("Calibrate Infinite Improbability", resTask.getBody().get(0).title());
    }

    @Test
    @DisplayName("Search strictly preserves tenant isolation: Tenant A never finds Tenant B entities")
    void searchTenantIsolation() {
        TenantRecord tenantA = provisionTenant("search-iso-a");
        TenantRecord tenantB = provisionTenant("search-iso-b");

        String tokenA = createAndLoginUser(tenantA, "usera@tenant-a.com", Role.USER, "UniqueAlphaPerson");
        createAndLoginUser(tenantB, "userb@tenant-b.com", Role.USER, "SecretBetaIndividual");

        ResponseEntity<List<SearchResultDto>> res = restTemplate.exchange(
                "/api/search?q=SecretBetaIndividual",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().isEmpty(), "Tenant A search must NEVER return Tenant B records");
    }
}
