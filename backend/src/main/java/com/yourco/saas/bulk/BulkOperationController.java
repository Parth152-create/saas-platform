package com.yourco.saas.bulk;

import com.yourco.saas.bulk.dto.BulkOperationResultDto;
import com.yourco.saas.bulk.dto.BulkTaskAssignRequest;
import com.yourco.saas.bulk.dto.BulkTaskStatusRequest;
import com.yourco.saas.bulk.dto.BulkUserStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bulk")
public class BulkOperationController {

    private final BulkOperationService bulkOperationService;

    public BulkOperationController(BulkOperationService bulkOperationService) {
        this.bulkOperationService = bulkOperationService;
    }

    @PostMapping("/tasks/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BulkOperationResultDto> bulkUpdateTaskStatus(@RequestBody @Valid BulkTaskStatusRequest request) {
        return ResponseEntity.ok(bulkOperationService.bulkUpdateTaskStatus(request));
    }

    @PostMapping("/tasks/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<BulkOperationResultDto> bulkAssignTasks(@RequestBody @Valid BulkTaskAssignRequest request) {
        return ResponseEntity.ok(bulkOperationService.bulkAssignTasks(request));
    }

    @PostMapping("/users/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkOperationResultDto> bulkUpdateUserStatus(@RequestBody @Valid BulkUserStatusRequest request) {
        return ResponseEntity.ok(bulkOperationService.bulkUpdateUserStatus(request));
    }
}
