package com.yourco.saas.domain.collaboration;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "direct_conversations")
public class DirectConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_one_id", nullable = false)
    private UUID userOneId;

    @Column(name = "user_two_id", nullable = false)
    private UUID userTwoId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DirectConversation() {}

    public static DirectConversation between(UUID u1, UUID u2) {
        DirectConversation convo = new DirectConversation();
        if (u1.toString().compareTo(u2.toString()) < 0) {
            convo.userOneId = u1;
            convo.userTwoId = u2;
        } else {
            convo.userOneId = u2;
            convo.userTwoId = u1;
        }
        return convo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserOneId() {
        return userOneId;
    }

    public UUID getUserTwoId() {
        return userTwoId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean includesUser(UUID userId) {
        return userOneId.equals(userId) || userTwoId.equals(userId);
    }

    public UUID getOtherUser(UUID userId) {
        if (userOneId.equals(userId)) return userTwoId;
        if (userTwoId.equals(userId)) return userOneId;
        return null;
    }
}
