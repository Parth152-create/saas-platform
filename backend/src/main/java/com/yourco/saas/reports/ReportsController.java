package com.yourco.saas.reports;

import com.yourco.saas.billing.RequiresFeature;
import com.yourco.saas.domain.billing.Feature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

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
}
