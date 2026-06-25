package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;

public class CheckoutRequest {

    private SubscriptionPlan plan;

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }
}
