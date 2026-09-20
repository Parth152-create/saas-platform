package com.yourco.saas.projects;

import com.yourco.saas.billing.RequiresFeature;
import com.yourco.saas.domain.billing.Feature;
import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectStatus;
import com.yourco.saas.projects.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiresFeature(Feature.PROJECT_MANAGEMENT)
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) ProjectPriority priority,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(projectService.getProjects(status, priority, ownerId, search));
    }

    @GetMapping("/stats")
    public ResponseEntity<ProjectStatsResponse> getStats() {
        return ResponseEntity.ok(projectService.getStats());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<ProjectActivityDto>> getGlobalActivity() {
        return ResponseEntity.ok(projectService.getRecentActivities(null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
        ProjectResponse created = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMemberDto>> listProjectMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProjectMembers(id));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProjectMemberDto> addProjectMember(
            @PathVariable UUID id,
            @RequestBody @Valid AddProjectMemberRequest request) {
        ProjectMemberDto member = projectService.addProjectMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> removeProjectMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId) {
        projectService.removeProjectMember(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<ProjectActivityDto>> getProjectActivity(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getRecentActivities(id));
    }
}
