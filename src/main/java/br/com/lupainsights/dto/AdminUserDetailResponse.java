package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdminUserDetailResponse {

    private UUID id;
    private String nome;
    private String email;
    private SubscriptionPlan plan;
    private UserRole role;
    private Instant createdAt;
    private boolean enabled;
    private boolean trialUtilizado;
    private Instant trialAte;
    private Instant planValidUntil;
    private Instant planCancelledAt;
    private boolean autoRenew;
    private long importJobsCount;
    private long rowsProcessed;
    private long rowsSuccess;
    private long rowsErrors;
    private long revenueCents;
    private AdminUsageSnapshotResponse usageLifetime;
    private AdminUsageSnapshotResponse usageLast30Days;
    private List<AdminImportJobSummaryResponse> recentImports;
    private List<AdminPaymentSummaryResponse> payments;
    private List<AdminDailyUsageResponse> dailyUsage;

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

    public boolean isTrialUtilizado() {
        return trialUtilizado;
    }

    public void setTrialUtilizado(boolean trialUtilizado) {
        this.trialUtilizado = trialUtilizado;
    }

    public Instant getTrialAte() {
        return trialAte;
    }

    public void setTrialAte(Instant trialAte) {
        this.trialAte = trialAte;
    }

    public Instant getPlanValidUntil() {
        return planValidUntil;
    }

    public void setPlanValidUntil(Instant planValidUntil) {
        this.planValidUntil = planValidUntil;
    }

    public Instant getPlanCancelledAt() {
        return planCancelledAt;
    }

    public void setPlanCancelledAt(Instant planCancelledAt) {
        this.planCancelledAt = planCancelledAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
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

    public long getRowsSuccess() {
        return rowsSuccess;
    }

    public void setRowsSuccess(long rowsSuccess) {
        this.rowsSuccess = rowsSuccess;
    }

    public long getRowsErrors() {
        return rowsErrors;
    }

    public void setRowsErrors(long rowsErrors) {
        this.rowsErrors = rowsErrors;
    }

    public long getRevenueCents() {
        return revenueCents;
    }

    public void setRevenueCents(long revenueCents) {
        this.revenueCents = revenueCents;
    }

    public AdminUsageSnapshotResponse getUsageLifetime() {
        return usageLifetime;
    }

    public void setUsageLifetime(AdminUsageSnapshotResponse usageLifetime) {
        this.usageLifetime = usageLifetime;
    }

    public AdminUsageSnapshotResponse getUsageLast30Days() {
        return usageLast30Days;
    }

    public void setUsageLast30Days(AdminUsageSnapshotResponse usageLast30Days) {
        this.usageLast30Days = usageLast30Days;
    }

    public List<AdminImportJobSummaryResponse> getRecentImports() {
        return recentImports;
    }

    public void setRecentImports(List<AdminImportJobSummaryResponse> recentImports) {
        this.recentImports = recentImports;
    }

    public List<AdminPaymentSummaryResponse> getPayments() {
        return payments;
    }

    public void setPayments(List<AdminPaymentSummaryResponse> payments) {
        this.payments = payments;
    }

    public List<AdminDailyUsageResponse> getDailyUsage() {
        return dailyUsage;
    }

    public void setDailyUsage(List<AdminDailyUsageResponse> dailyUsage) {
        this.dailyUsage = dailyUsage;
    }
}
