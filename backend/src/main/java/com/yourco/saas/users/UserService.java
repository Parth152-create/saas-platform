package com.yourco.saas.users;

import com.yourco.saas.auth.JwtService;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.users.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditLogRepository auditLogRepository;

    public UserService(UserRepository userRepository,
                       JwtService jwtService,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public UserResponse changeRole(UUID targetUserId, Role newRole) {
        if (newRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target role is required");
        }
        if (newRole == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPER_ADMIN role cannot be assigned through user management");
        }

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        if (targetUserId.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change your own role");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in tenant"));

        if (targetUser.getStatus() == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change role of a deactivated user");
        }

        Role currentRole = targetUser.getRole();
        if (currentRole == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER_ADMIN role cannot be modified");
        }

        // Validate authority hierarchy
        if (actorRole == Role.SUPER_ADMIN) {
            // SUPER_ADMIN can manage ADMIN, MANAGER, USER to ADMIN, MANAGER, USER
        } else if (actorRole == Role.ADMIN) {
            if (currentRole == Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can manage ADMIN users");
            }
            if (newRole == Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can promote users to ADMIN");
            }
            if ((currentRole != Role.USER && currentRole != Role.MANAGER) ||
                (newRole != Role.USER && newRole != Role.MANAGER)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN can only change roles between USER and MANAGER");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions to change user roles");
        }

        targetUser.setRole(newRole);
        User saved = userRepository.save(targetUser);

        // Invalidate active refresh tokens
        jwtService.revokeUserRefreshTokens(targetUserId);

        // Audit log
        String details = String.format("target_user=%s, previous_role=%s, new_role=%s",
                targetUserId, currentRole, newRole);
        auditLogRepository.save(new AuditLog(actorId, actorRole.name(), "USER_ROLE_CHANGE", "SUCCESS", details));

        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse deactivateUser(UUID targetUserId) {
        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        if (targetUserId.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate your own account");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in tenant"));

        if (targetUser.getStatus() == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already deactivated");
        }

        if (targetUser.getRole() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER_ADMIN users cannot be deactivated");
        }

        if (targetUser.getRole() == Role.ADMIN && actorRole != Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can deactivate ADMIN users");
        }

        if (actorRole != Role.ADMIN && actorRole != Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions to deactivate users");
        }

        targetUser.setStatus(UserStatus.DISABLED);
        User saved = userRepository.save(targetUser);

        // Invalidate session state
        jwtService.markUserDisabled(targetUserId);

        // Audit log
        String details = String.format("target_user=%s, email=%s", targetUserId, targetUser.getEmail());
        auditLogRepository.save(new AuditLog(actorId, actorRole.name(), "USER_DEACTIVATE", "SUCCESS", details));

        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse reactivateUser(UUID targetUserId) {
        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in tenant"));

        if (targetUser.getStatus() != UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not deactivated");
        }

        if (targetUser.getRole() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER_ADMIN users cannot be modified through user reactivation");
        }

        if (targetUser.getRole() == Role.ADMIN && actorRole != Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can reactivate ADMIN users");
        }

        if (actorRole != Role.ADMIN && actorRole != Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions to reactivate users");
        }

        targetUser.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(targetUser);

        // Clear disabled session state in Redis
        jwtService.markUserEnabled(targetUserId);

        // Audit log
        String details = String.format("target_user=%s, email=%s", targetUserId, targetUser.getEmail());
        auditLogRepository.save(new AuditLog(actorId, actorRole.name(), "USER_REACTIVATE", "SUCCESS", details));

        return toUserResponse(saved);
    }

    private UserResponse toUserResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getRole(),
                u.getStatus(),
                u.getInviteToken(),
                u.getInviteTokenExpiresAt(),
                u.getCreatedAt()
        );
    }

    private UUID currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user principal");
        }
    }

    private Role currentActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String authority = ga.getAuthority();
            if (authority.startsWith("ROLE_")) {
                try {
                    return Role.valueOf(authority.substring(5));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role authority not found");
    }
}
