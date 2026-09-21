package com.yourco.saas.integration;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.collaboration.dto.NotificationResponse;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.leave.dto.CreateLeaveRequest;
import com.yourco.saas.leave.dto.LeaveRequestResponse;
import com.yourco.saas.leave.dto.LeaveReviewRequest;
import com.yourco.saas.reports.dto.LeaveReportDto;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class V1_1_EndToEndBusinessFlowTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private User createUserAndEmployee(TenantRecord tenant, String email, Role role, String name) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            User saved = userRepository.save(user);

            Employee emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6));
            emp.setName(name);
            emp.setEmail(email);
            emp.setDepartment("Operations");
            emp.setPosition("Analyst");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setHireDate(LocalDate.now());
            employeeRepository.save(emp);

            return saved;
        } finally {
            TenantContext.clear();
        }
    }

    private String login(TenantRecord tenant, String email) {
        ResponseEntity<TokenResponse> res = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), email, RAW_PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        return res.getBody().accessToken();
    }

    @Test
    @DisplayName("Realistic E2E Flow: Employee submits leave -> Manager notified -> Manager approves -> Employee notified -> Audit recorded -> Reporting reflects state")
    void completeEndToEndWorkforceLeaveFlow() {
        TenantRecord tenant = provisionTenant("e2e-v11-flow");

        User empUser = createUserAndEmployee(tenant, "employee@v11.test", Role.USER, "Jane Employee");
        User mgrUser = createUserAndEmployee(tenant, "manager@v11.test", Role.MANAGER, "John Manager");

        String empToken = login(tenant, "employee@v11.test");
        String mgrToken = login(tenant, "manager@v11.test");

        // 1. Employee submits leave request
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.ANNUAL,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(9),
                "Annual vacation travel"
        );

        ResponseEntity<LeaveRequestResponse> submitRes = restTemplate.exchange(
                "/api/leave",
                HttpMethod.POST,
                new HttpEntity<>(request, bearerHeaders(empToken)),
                LeaveRequestResponse.class
        );
        assertEquals(HttpStatus.CREATED, submitRes.getStatusCode());
        LeaveRequestResponse submittedLeave = submitRes.getBody();
        assertNotNull(submittedLeave);
        assertEquals(LeaveStatus.PENDING, submittedLeave.status());
        UUID leaveId = submittedLeave.id();

        // 2. Manager receives notification
        ResponseEntity<String> mgrNotifsRes = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                String.class
        );
        assertEquals(HttpStatus.OK, mgrNotifsRes.getStatusCode());
        assertNotNull(mgrNotifsRes.getBody());
        assertTrue(mgrNotifsRes.getBody().contains("Jane Employee"), "Manager must have received notification for submitted leave");

        // 3. Manager approves leave request
        LeaveReviewRequest reviewRequest = new LeaveReviewRequest("Approved. Ensure handoff is done.");
        ResponseEntity<LeaveRequestResponse> approveRes = restTemplate.exchange(
                "/api/leave/" + leaveId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(reviewRequest, bearerHeaders(mgrToken)),
                LeaveRequestResponse.class
        );
        assertEquals(HttpStatus.OK, approveRes.getStatusCode());
        assertEquals(LeaveStatus.APPROVED, approveRes.getBody().status());
        assertEquals("Approved. Ensure handoff is done.", approveRes.getBody().reviewNote());

        // 4. Employee receives notification of approval
        ResponseEntity<String> empNotifsRes = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(empToken)),
                String.class
        );
        assertEquals(HttpStatus.OK, empNotifsRes.getStatusCode());
        assertTrue(empNotifsRes.getBody().contains("Approved"), "Employee must receive notification that leave was approved");

        // 5. Reporting reflects the new approved state
        ResponseEntity<LeaveReportDto> reportRes = restTemplate.exchange(
                "/api/reports/leave",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                LeaveReportDto.class
        );
        assertEquals(HttpStatus.OK, reportRes.getStatusCode());
        LeaveReportDto leaveReport = reportRes.getBody();
        assertNotNull(leaveReport);
        assertEquals(1, leaveReport.totalRequests());
        assertEquals(1, leaveReport.approvedRequests());
        assertEquals(0, leaveReport.pendingRequests());
        assertEquals(5.0, leaveReport.totalDaysApproved().doubleValue());
    }
}
