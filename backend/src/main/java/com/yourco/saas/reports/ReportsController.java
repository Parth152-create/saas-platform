package com.yourco.saas.reports;

import com.yourco.saas.billing.RequiresFeature;
import com.yourco.saas.domain.billing.Feature;
import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;
import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectStatus;
import com.yourco.saas.domain.projects.TaskPriority;
import com.yourco.saas.domain.projects.TaskStatus;
import com.yourco.saas.reports.dto.LeaveReportDto;
import com.yourco.saas.reports.dto.ProjectsReportDto;
import com.yourco.saas.reports.dto.TasksReportDto;
import com.yourco.saas.reports.dto.WorkforceReportDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/basic")
    @RequiresFeature(Feature.BASIC_REPORTS)
    public ResponseEntity<Map<String, Object>> getBasicReports() {
        return ResponseEntity.ok(Map.of(
                "reportType", "BASIC",
                "status", "available",
                "data", List.of("Headcount Summary", "Attendance Log")
        ));
    }

    @GetMapping("/advanced")
    @RequiresFeature(Feature.ADVANCED_REPORTS)
    public ResponseEntity<Map<String, Object>> getAdvancedReports() {
        return ResponseEntity.ok(Map.of(
                "reportType", "ADVANCED",
                "status", "available",
                "data", List.of("Monthly Workforce Cost & Utilization", "PTO Accrual Projection")
        ));
    }

    @GetMapping("/analytics")
    @RequiresFeature(Feature.ADVANCED_ANALYTICS)
    public ResponseEntity<Map<String, Object>> getAdvancedAnalytics() {
        return ResponseEntity.ok(Map.of(
                "reportType", "ANALYTICS",
                "status", "available",
                "metrics", Map.of("headcountGrowth", 0.15, "runRate", 248000)
        ));
    }

    @GetMapping("/custom-workflows")
    @RequiresFeature(Feature.CUSTOM_WORKFLOWS)
    public ResponseEntity<Map<String, Object>> getCustomWorkflows() {
        return ResponseEntity.ok(Map.of(
                "reportType", "WORKFLOWS",
                "status", "available",
                "workflows", List.of("Custom Approval Matrix", "Multi-step Escalation")
        ));
    }

    @PostMapping("/admin-advanced-export")
    @PreAuthorize("hasRole('ADMIN')")
    @RequiresFeature(Feature.ADVANCED_REPORTS)
    public ResponseEntity<Map<String, Object>> exportAdvancedReportAsAdmin() {
        return ResponseEntity.ok(Map.of(
                "exportStatus", "SUCCESS",
                "authorizedRole", "ADMIN",
                "feature", "ADVANCED_REPORTS"
        ));
    }

    @GetMapping("/workforce")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkforceReportDto> getWorkforceReport(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(reportService.getWorkforceReport(department, status));
    }

    @GetMapping("/projects")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProjectsReportDto> getProjectsReport(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) ProjectPriority priority) {
        return ResponseEntity.ok(reportService.getProjectsReport(status, priority));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TasksReportDto> getTasksReport(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {
        return ResponseEntity.ok(reportService.getTasksReport(projectId, status, priority));
    }

    @GetMapping("/leave")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LeaveReportDto> getLeaveReport(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType leaveType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getLeaveReport(status, leaveType, fromDate, toDate));
    }

    @GetMapping("/export/{type}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String type) {
        String csvContent = switch (type.toLowerCase()) {
            case "workforce" -> reportService.exportWorkforceCsv();
            case "projects" -> reportService.exportProjectsCsv();
            case "tasks" -> reportService.exportTasksCsv();
            case "leave" -> reportService.exportLeaveCsv();
            default -> throw new IllegalArgumentException("Unknown report type: " + type);
        };

        byte[] bytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + type.toLowerCase() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
