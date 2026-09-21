package com.yourco.saas.leave;

import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;
import com.yourco.saas.leave.dto.CreateLeaveRequest;
import com.yourco.saas.leave.dto.LeaveBalanceResponse;
import com.yourco.saas.leave.dto.LeaveRequestResponse;
import com.yourco.saas.leave.dto.LeaveReviewRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        return ResponseEntity.ok(leaveService.getMyLeaveRequests());
    }

    @GetMapping("/balances")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyLeaveBalances() {
        return ResponseEntity.ok(leaveService.getMyLeaveBalances());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<LeaveRequestResponse>> getPendingLeaveRequests() {
        return ResponseEntity.ok(leaveService.getPendingLeaveRequests());
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<LeaveRequestResponse>> getAllLeaveRequests(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(leaveService.getAllLeaveRequests(status, type, fromDate, toDate, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LeaveRequestResponse> getLeaveRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(leaveService.getLeaveRequestById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@RequestBody @Valid CreateLeaveRequest request) {
        LeaveRequestResponse created = leaveService.createLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LeaveRequestResponse> cancelLeaveRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(leaveService.cancelLeaveRequest(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveReviewRequest request) {
        return ResponseEntity.ok(leaveService.approveLeaveRequest(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveReviewRequest request) {
        return ResponseEntity.ok(leaveService.rejectLeaveRequest(id, request));
    }
}
