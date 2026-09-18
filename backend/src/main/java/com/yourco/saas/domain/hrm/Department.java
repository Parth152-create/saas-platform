package com.yourco.saas.domain.hrm;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 100)
    private String lead;

    @Column(name = "budget_utilization", precision = 5, scale = 2)
    private BigDecimal budgetUtilization = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Department() {}

    public Department(String name, String lead, BigDecimal budgetUtilization) {
        this.name = name;
        this.lead = lead;
        this.budgetUtilization = budgetUtilization != null ? budgetUtilization : BigDecimal.ZERO;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLead() {
        return lead;
    }

    public void setLead(String lead) {
        this.lead = lead;
    }

    public BigDecimal getBudgetUtilization() {
        return budgetUtilization;
    }

    public void setBudgetUtilization(BigDecimal budgetUtilization) {
        this.budgetUtilization = budgetUtilization;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
