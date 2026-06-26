package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public class AdminUserSummaryResponse {

    private UUID id;
    private String nome;
    private String email;
    private SubscriptionPlan plan;
    private UserRole role;
    private Instant createdAt;
    private boolean enabled;
    private long importJobsCount;
    private long rowsProcessed;
    private long revenueCents;
    private AdminUsageSnapshotResponse usageToday;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getImportJobsCount() {
        return importJobsCount;
    }

    public void setImportJobsCount(long importJobsCount) {
        this.importJobsCount = importJobsCount;
    }

    public long getRowsProcessed() {
        return rowsProcessed;
    }

    public void setRowsProcessed(long rowsProcessed) {
        this.rowsProcessed = rowsProcessed;
    }

    public long getRevenueCents() {
        return revenueCents;
    }

    public void setRevenueCents(long revenueCents) {
        this.revenueCents = revenueCents;
    }

    public AdminUsageSnapshotResponse getUsageToday() {
        return usageToday;
    }

    public void setUsageToday(AdminUsageSnapshotResponse usageToday) {
        this.usageToday = usageToday;
    }
}
