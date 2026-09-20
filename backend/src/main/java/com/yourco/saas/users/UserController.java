package com.yourco.saas.users;

import com.yourco.saas.common.email.EmailService;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRegistryService;
import com.yourco.saas.users.dto.ChangeRoleRequest;
import com.yourco.saas.users.dto.InviteUserRequest;
import com.yourco.saas.users.dto.InviteUserResponse;
import com.yourco.saas.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TenantRegistryService tenantRegistryService;
    private final EmailService emailService;
    private final UserService userService;

    public UserController(UserRepository userRepository,
                          TenantRegistryService tenantRegistryService,
                          EmailService emailService,
                          UserService userService) {
        this.userRepository = userRepository;
        this.tenantRegistryService = tenantRegistryService;
        this.emailService = emailService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getRole(),
                        u.getStatus(),
                        u.getInviteToken(),
                        u.getInviteTokenExpiresAt(),
                        u.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InviteUserResponse> inviteUser(@RequestBody @Valid InviteUserRequest request) {
        if (request.role() == Role.SUPER_ADMIN && !isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can invite SUPER_ADMIN users");
        }

        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists in this tenant");
        }

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofDays(7));

        User user = User.newInvitedUser(request.email(), request.role(), token, expiresAt);
        userRepository.save(user);

        String currentSchema = TenantContext.getTenant();
        if (currentSchema != null) {
            tenantRegistryService.findBySchemaName(currentSchema).ifPresent(tenant ->
                    emailService.sendInvitationEmail(user.getEmail(), tenant.tenantId(), token, user.getRole())
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new InviteUserResponse(user.getEmail(), user.getRole(), token, expiresAt));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable UUID userId,
            @RequestBody @Valid ChangeRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(userId, request.role()));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody @Valid ChangeRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(userId, request.role()));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.deactivateUser(userId));
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivateUserPost(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.deactivateUser(userId));
    }

    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> reactivateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.reactivateUser(userId));
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}