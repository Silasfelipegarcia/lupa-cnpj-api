package br.com.lupainsights.dto;

import java.util.List;

public class AdminOverviewResponse {

    private int periodDays;
    private long totalUsers;
    private long newUsersInPeriod;
    private List<AdminPlanCountResponse> usersByPlan;
    private long activeTrials;
    private long activePaidSubscriptions;
    private long totalRevenueCents;
    private long revenueInPeriodCents;
    private long approvedPaymentsInPeriod;
    private long pendingPayments;
    private long totalImportJobs;
    private long activeImportJobs;
    private long totalRowsProcessed;
    private long totalRowsSuccess;
    private long totalRowsErrors;
    private AdminUsageSnapshotResponse usageInPeriod;
    private AdminUsageSnapshotResponse usageToday;

    public int getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(int periodDays) {
        this.periodDays = periodDays;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getNewUsersInPeriod() {
        return newUsersInPeriod;
    }

    public void setNewUsersInPeriod(long newUsersInPeriod) {
        this.newUsersInPeriod = newUsersInPeriod;
    }

    public List<AdminPlanCountResponse> getUsersByPlan() {
        return usersByPlan;
    }

    public void setUsersByPlan(List<AdminPlanCountResponse> usersByPlan) {
        this.usersByPlan = usersByPlan;
    }

    public long getActiveTrials() {
        return activeTrials;
    }

    public void setActiveTrials(long activeTrials) {
        this.activeTrials = activeTrials;
    }

    public long getActivePaidSubscriptions() {
        return activePaidSubscriptions;
    }

    public void setActivePaidSubscriptions(long activePaidSubscriptions) {
        this.activePaidSubscriptions = activePaidSubscriptions;
    }

    public long getTotalRevenueCents() {
        return totalRevenueCents;
    }

    public void setTotalRevenueCents(long totalRevenueCents) {
        this.totalRevenueCents = totalRevenueCents;
    }

    public long getRevenueInPeriodCents() {
        return revenueInPeriodCents;
    }

    public void setRevenueInPeriodCents(long revenueInPeriodCents) {
        this.revenueInPeriodCents = revenueInPeriodCents;
    }

    public long getApprovedPaymentsInPeriod() {
        return approvedPaymentsInPeriod;
    }

    public void setApprovedPaymentsInPeriod(long approvedPaymentsInPeriod) {
        this.approvedPaymentsInPeriod = approvedPaymentsInPeriod;
    }

    public long getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(long pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public long getTotalImportJobs() {
        return totalImportJobs;
    }

    public void setTotalImportJobs(long totalImportJobs) {
        this.totalImportJobs = totalImportJobs;
    }

    public long getActiveImportJobs() {
        return activeImportJobs;
    }

    public void setActiveImportJobs(long activeImportJobs) {
        this.activeImportJobs = activeImportJobs;
    }

    public long getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public void setTotalRowsProcessed(long totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }

    public long getTotalRowsSuccess() {
        return totalRowsSuccess;
    }

    public void setTotalRowsSuccess(long totalRowsSuccess) {
        this.totalRowsSuccess = totalRowsSuccess;
    }

    public long getTotalRowsErrors() {
        return totalRowsErrors;
    }

    public void setTotalRowsErrors(long totalRowsErrors) {
        this.totalRowsErrors = totalRowsErrors;
    }

    public AdminUsageSnapshotResponse getUsageInPeriod() {
        return usageInPeriod;
    }

    public void setUsageInPeriod(AdminUsageSnapshotResponse usageInPeriod) {
        this.usageInPeriod = usageInPeriod;
    }

    public AdminUsageSnapshotResponse getUsageToday() {
        return usageToday;
    }

    public void setUsageToday(AdminUsageSnapshotResponse usageToday) {
        this.usageToday = usageToday;
    }
}
