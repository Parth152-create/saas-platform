package com.yourco.saas.leave;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.collaboration.Notification;
import com.yourco.saas.domain.collaboration.NotificationRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.leave.LeaveRequest;
import com.yourco.saas.domain.leave.LeaveRequestRepository;
import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.leave.dto.CreateLeaveRequest;
import com.yourco.saas.leave.dto.LeaveBalanceResponse;
import com.yourco.saas.leave.dto.LeaveRequestResponse;
import com.yourco.saas.leave.dto.LeaveReviewRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class LeaveWorkflowIntegrationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    LeaveRequestRepository leaveRequestRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createAndLoginUser(TenantRecord tenant, String email, Role role, UserStatus status) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(status);
            userRepository.save(user);

            Employee emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6));
            emp.setName(email.split("@")[0]);
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
    @DisplayName("Employee creates leave request, manager approves, balances and notifications update")
    void fullLeaveApprovalLifecycle() {
        TenantRecord tenant = provisionTenant("leave-full-flow");

        String empToken = createAndLoginUser(tenant, "employee@flow.test", Role.USER, UserStatus.ACTIVE);
        String mgrToken = createAndLoginUser(tenant, "manager@flow.test", Role.MANAGER, UserStatus.ACTIVE);

        // 1. Employee creates leave request
        CreateLeaveRequest createReq = new CreateLeaveRequest(
                LeaveType.ANNUAL,
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(4),
                "Summer vacation trip"
        );

        ResponseEntity<LeaveRequestResponse> createRes = restTemplate.exchange(
                "/api/leave",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(empToken)),
                LeaveRequestResponse.class
        );

        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        LeaveRequestResponse created = createRes.getBody();
        assertNotNull(created);
        assertEquals(LeaveStatus.PENDING, created.status());
        assertEquals(LeaveType.ANNUAL, created.leaveType());
        assertEquals(3.0, created.daysCount().doubleValue());
        UUID leaveId = created.id();

        // 2. Check Employee balances - pendingDays should be 3
        ResponseEntity<List<LeaveBalanceResponse>> balRes = restTemplate.exchange(
                "/api/leave/balances",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(empToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, balRes.getStatusCode());
        List<LeaveBalanceResponse> balances = balRes.getBody();
        assertNotNull(balances);
        LeaveBalanceResponse annualBal = balances.stream()
                .filter(b -> b.leaveType() == LeaveType.ANNUAL)
                .findFirst().orElseThrow();
        assertEquals(3.0, annualBal.pendingDays().doubleValue());

        // 3. Manager approves leave request
        LeaveReviewRequest reviewReq = new LeaveReviewRequest("Approved, have fun!");
        ResponseEntity<LeaveRequestResponse> approveRes = restTemplate.exchange(
                "/api/leave/" + leaveId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(reviewReq, bearerHeaders(mgrToken)),
                LeaveRequestResponse.class
        );

        assertEquals(HttpStatus.OK, approveRes.getStatusCode());
        LeaveRequestResponse approved = approveRes.getBody();
        assertNotNull(approved);
        assertEquals(LeaveStatus.APPROVED, approved.status());
        assertEquals("Approved, have fun!", approved.reviewNote());
        assertNotNull(approved.reviewedAt());

        // 4. Check balances after approval - usedDays should be 3, pendingDays should be 0
        ResponseEntity<List<LeaveBalanceResponse>> balResAfter = restTemplate.exchange(
                "/api/leave/balances",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(empToken)),
                new ParameterizedTypeReference<>() {}
        );
        LeaveBalanceResponse annualBalAfter = balResAfter.getBody().stream()
                .filter(b -> b.leaveType() == LeaveType.ANNUAL)
                .findFirst().orElseThrow();
        assertEquals(0.0, annualBalAfter.pendingDays().doubleValue());
        assertEquals(3.0, annualBalAfter.usedDays().doubleValue());
        assertEquals(17.0, annualBalAfter.remainingDays().doubleValue());

        // 5. Verify notifications and audit logs in tenant schema
        TenantContext.setTenant(tenant.schemaName());
        try {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertTrue(auditLogs.stream().anyMatch(a -> "LEAVE_CREATED".equals(a.getAction())));
            assertTrue(auditLogs.stream().anyMatch(a -> "LEAVE_APPROVED".equals(a.getAction())));

            List<Notification> notifications = notificationRepository.findAll();
            assertTrue(notifications.stream().anyMatch(n -> n.getMessage().contains("Approved") || n.getTitle().contains("Approved")));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Employee cannot approve their own leave request (403 Forbidden)")
    void userCannotApproveOwnLeave() {
        TenantRecord tenant = provisionTenant("leave-self-approve");

        String empToken = createAndLoginUser(tenant, "selfapprove@test.com", Role.MANAGER, UserStatus.ACTIVE);

        CreateLeaveRequest createReq = new CreateLeaveRequest(
                LeaveType.SICK,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "Doctor appointment"
        );

        ResponseEntity<LeaveRequestResponse> createRes = restTemplate.exchange(
                "/api/leave",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(empToken)),
                LeaveRequestResponse.class
        );
        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        UUID leaveId = createRes.getBody().id();

        // Attempt to self-approve
        ResponseEntity<String> approveRes = restTemplate.exchange(
                "/api/leave/" + leaveId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(new LeaveReviewRequest("Self approved"), bearerHeaders(empToken)),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, approveRes.getStatusCode(),
                "User must receive 403 Forbidden when attempting to self-approve leave");
    }

    @Test
    @DisplayName("Employee can cancel their own pending leave request")
    void employeeCanCancelOwnLeave() {
        TenantRecord tenant = provisionTenant("leave-cancel");

        String empToken = createAndLoginUser(tenant, "canceller@test.com", Role.USER, UserStatus.ACTIVE);

        CreateLeaveRequest createReq = new CreateLeaveRequest(
                LeaveType.CASUAL,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6),
                "Personal errands"
        );

        ResponseEntity<LeaveRequestResponse> createRes = restTemplate.exchange(
                "/api/leave",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(empToken)),
                LeaveRequestResponse.class
        );
        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        UUID leaveId = createRes.getBody().id();

        // Cancel
        ResponseEntity<LeaveRequestResponse> cancelRes = restTemplate.exchange(
                "/api/leave/" + leaveId + "/cancel",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(empToken)),
                LeaveRequestResponse.class
        );

        assertEquals(HttpStatus.OK, cancelRes.getStatusCode());
        assertEquals(LeaveStatus.CANCELLED, cancelRes.getBody().status());
    }

    @Test
    @DisplayName("Cross-tenant leave access is strictly rejected (404 Not Found)")
    void crossTenantLeaveIsolation() {
        TenantRecord tenantA = provisionTenant("leave-iso-a");
        TenantRecord tenantB = provisionTenant("leave-iso-b");

        String tokenA = createAndLoginUser(tenantA, "user@tenant-a.com", Role.ADMIN, UserStatus.ACTIVE);
        String tokenB = createAndLoginUser(tenantB, "user@tenant-b.com", Role.USER, UserStatus.ACTIVE);

        // User in Tenant B creates leave
        CreateLeaveRequest createReq = new CreateLeaveRequest(
                LeaveType.ANNUAL,
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(4),
                "Tenant B vacation"
        );

        ResponseEntity<LeaveRequestResponse> createRes = restTemplate.exchange(
                "/api/leave",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(tokenB)),
                LeaveRequestResponse.class
        );
        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        UUID leaveBId = createRes.getBody().id();

        // User A (even though ADMIN in Tenant A) tries to get Tenant B leave request
        ResponseEntity<String> getRes = restTemplate.exchange(
                "/api/leave/" + leaveBId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getRes.getStatusCode());

        // User A tries to approve Tenant B leave request
        ResponseEntity<String> approveRes = restTemplate.exchange(
                "/api/leave/" + leaveBId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(new LeaveReviewRequest("Malicious approval"), bearerHeaders(tokenA)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, approveRes.getStatusCode());
    }
}
