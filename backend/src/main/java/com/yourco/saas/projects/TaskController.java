package com.yourco.saas.projects;

import com.yourco.saas.billing.RequiresFeature;
import com.yourco.saas.domain.billing.Feature;
import com.yourco.saas.domain.projects.TaskPriority;
import com.yourco.saas.domain.projects.TaskStatus;
import com.yourco.saas.projects.dto.CreateTaskRequest;
import com.yourco.saas.projects.dto.TaskResponse;
import com.yourco.saas.projects.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiresFeature(Feature.TASK_MANAGEMENT)
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> listProjectTasks(
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId, status, priority, assigneeId, overdue, search));
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID projectId,
            @RequestBody @Valid CreateTaskRequest request) {
        TaskResponse created = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    @PatchMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @RequestBody @Valid UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @DeleteMapping("/api/tasks/{taskId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<List<TaskResponse>> listAllTasks(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(taskService.getAllTasks(projectId, status, priority, assigneeId, overdue, search));
    }
}
