package com.yourco.saas.workload;

import com.yourco.saas.workload.dto.DepartmentWorkloadDto;
import com.yourco.saas.workload.dto.EmployeeWorkloadDto;
import com.yourco.saas.workload.dto.ProjectWorkloadDto;
import com.yourco.saas.workload.dto.WorkforceWorkloadSummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workload")
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkforceWorkloadSummaryDto> getWorkforceSummary() {
        return ResponseEntity.ok(workloadService.getWorkforceSummary());
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<EmployeeWorkloadDto>> getEmployeeWorkloads() {
        return ResponseEntity.ok(workloadService.getEmployeeWorkloads());
    }

    @GetMapping("/projects")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProjectWorkloadDto>> getProjectWorkloads() {
        return ResponseEntity.ok(workloadService.getProjectWorkloads());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<EmployeeWorkloadDto> getMyWorkload() {
        return ResponseEntity.ok(workloadService.getMyWorkload());
    }
}
