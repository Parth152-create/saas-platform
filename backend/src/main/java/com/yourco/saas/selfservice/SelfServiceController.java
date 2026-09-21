package com.yourco.saas.selfservice;

import com.yourco.saas.projects.dto.ProjectResponse;
import com.yourco.saas.projects.dto.TaskResponse;
import com.yourco.saas.selfservice.dto.SelfServiceOverviewDto;
import com.yourco.saas.selfservice.dto.SelfServiceProfileDto;
import com.yourco.saas.selfservice.dto.UpdateSelfServiceProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/self-service")
@PreAuthorize("hasRole('USER')")
public class SelfServiceController {

    private final SelfServiceService selfServiceService;

    public SelfServiceController(SelfServiceService selfServiceService) {
        this.selfServiceService = selfServiceService;
    }

    @GetMapping("/profile")
    public ResponseEntity<SelfServiceProfileDto> getProfile() {
        return ResponseEntity.ok(selfServiceService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<SelfServiceProfileDto> updateProfile(@RequestBody @Valid UpdateSelfServiceProfileRequest request) {
        return ResponseEntity.ok(selfServiceService.updateProfile(request));
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getAssignedTasks() {
        return ResponseEntity.ok(selfServiceService.getAssignedTasks());
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> getAssignedProjects() {
        return ResponseEntity.ok(selfServiceService.getAssignedProjects());
    }

    @GetMapping("/overview")
    public ResponseEntity<SelfServiceOverviewDto> getOverview() {
        return ResponseEntity.ok(selfServiceService.getOverview());
    }
}
