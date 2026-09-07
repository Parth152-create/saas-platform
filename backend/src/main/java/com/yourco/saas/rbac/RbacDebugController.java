package com.yourco.saas.rbac;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TEMPORARY — verifies RoleHierarchy wiring. Delete once real endpoints have their own @PreAuthorize.
@RestController
@RequestMapping("/api/rbac-debug")
public class RbacDebugController {

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> userLevel() {
        return ResponseEntity.ok("USER-or-above access granted");
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> managerLevel() {
        return ResponseEntity.ok("MANAGER-or-above access granted");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminLevel() {
        return ResponseEntity.ok("ADMIN-or-above access granted");
    }

    @GetMapping("/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> superAdminLevel() {
        return ResponseEntity.ok("SUPER_ADMIN access granted");
    }
}