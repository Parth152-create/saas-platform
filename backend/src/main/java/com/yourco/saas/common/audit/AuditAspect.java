package com.yourco.saas.common.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        UUID actorId = currentActorId();
        String actorRole = currentActorRole();

        try {
            Object result = joinPoint.proceed();
            save(actorId, actorRole, auditable.action(), "SUCCESS", null);
            return result;
        } catch (Throwable ex) {
            save(actorId, actorRole, auditable.action(), "FAILURE", ex.getMessage());
            throw ex;
        }
    }

    private void save(UUID actorId, String actorRole, String action, String outcome, String details) {
        try {
            auditLogRepository.save(new AuditLog(actorId, actorRole, action, outcome, details));
        } catch (Exception e) {
            log.error("Failed to write audit log for action {}: {}", action, e.getMessage());
        }
    }

    private UUID currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String currentActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
    }
}
