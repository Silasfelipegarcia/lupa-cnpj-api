package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;

public class AdminPlanCountResponse {

    private SubscriptionPlan plan;
    private long count;

    public AdminPlanCountResponse() {
    }

    public AdminPlanCountResponse(SubscriptionPlan plan, long count) {
        this.plan = plan;
        this.count = count;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
