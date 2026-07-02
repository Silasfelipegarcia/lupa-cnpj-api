package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;

public class CheckoutRequest {

    private SubscriptionPlan plan;
    /** Parcelas no cartão (1 = à vista, até 12). */
    private Integer installments = 1;

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }
}
