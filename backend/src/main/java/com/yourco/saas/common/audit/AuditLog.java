package com.yourco.saas.common.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {}

    public AuditLog(UUID actorId, String actorRole, String action, String outcome, String details) {
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.outcome = outcome;
        this.details = details;
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getOutcome() { return outcome; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
