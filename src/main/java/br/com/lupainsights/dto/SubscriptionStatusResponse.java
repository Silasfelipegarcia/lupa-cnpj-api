package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;

import java.time.Instant;

public class SubscriptionStatusResponse {

    private String status;
    private SubscriptionPlan plan;
    private String planNome;
    private Instant validUntil;
    private Instant cancelledAt;
    private boolean autoRenew;
    private long daysRemaining;
    private String defaultCardId;
    private boolean podeCancelar;
    private boolean podeReativar;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public String getPlanNome() {
        return planNome;
    }

    public void setPlanNome(String planNome) {
        this.planNome = planNome;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public String getDefaultCardId() {
        return defaultCardId;
    }

    public void setDefaultCardId(String defaultCardId) {
        this.defaultCardId = defaultCardId;
    }

    public boolean isPodeCancelar() {
        return podeCancelar;
    }

    public void setPodeCancelar(boolean podeCancelar) {
        this.podeCancelar = podeCancelar;
    }

    public boolean isPodeReativar() {
        return podeReativar;
    }

    public void setPodeReativar(boolean podeReativar) {
        this.podeReativar = podeReativar;
    }
}
