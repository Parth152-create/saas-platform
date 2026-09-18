package com.yourco.saas.hrm;

import com.yourco.saas.billing.RequiresFeature;
import com.yourco.saas.domain.billing.Feature;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.hrm.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hrm")
public class HrmController {

    private final HrmService hrmService;

    public HrmController(HrmService hrmService) {
        this.hrmService = hrmService;
    }

    @GetMapping("/employees")
    @RequiresFeature(Feature.EMPLOYEE_MANAGEMENT)
    public ResponseEntity<List<EmployeeDto>> listEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(hrmService.getEmployees(department, status, search));
    }

    @GetMapping("/employees/{id}")
    @RequiresFeature(Feature.EMPLOYEE_MANAGEMENT)
    public ResponseEntity<EmployeeDto> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(hrmService.getEmployeeById(id));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    @RequiresFeature(Feature.EMPLOYEE_MANAGEMENT)
    public ResponseEntity<EmployeeDto> createEmployee(@RequestBody @Valid CreateEmployeeRequest request) {
        EmployeeDto created = hrmService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequiresFeature(Feature.EMPLOYEE_MANAGEMENT)
    public ResponseEntity<EmployeeDto> updateEmployee(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateEmployeeRequest request) {
        return ResponseEntity.ok(hrmService.updateEmployee(id, request));
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequiresFeature(Feature.EMPLOYEE_MANAGEMENT)
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
        hrmService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/departments")
    @RequiresFeature(Feature.TEAM_MANAGEMENT)
    public ResponseEntity<List<DepartmentDto>> listDepartments() {
        return ResponseEntity.ok(hrmService.getDepartments());
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @RequiresFeature(Feature.TEAM_MANAGEMENT)
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody @Valid CreateDepartmentRequest request) {
        DepartmentDto created = hrmService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/stats")
    @RequiresFeature(Feature.EMPLOYEE_MANAGEMENT)
    public ResponseEntity<HrmStatsDto> getStats() {
        return ResponseEntity.ok(hrmService.getStats());
    }
}
