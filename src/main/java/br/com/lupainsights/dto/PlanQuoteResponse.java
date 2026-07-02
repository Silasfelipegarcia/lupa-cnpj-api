package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;

public class PlanQuoteResponse {

    private SubscriptionPlan plan;
    private int amountCents;
    private String amountLabel;
    private int fullPriceCents;
    private String fullPriceLabel;
    private int monthlyPriceCents;
    private int annualPriceCents;
    private Integer installments;
    private String installmentAmountLabel;
    private boolean upgrade;
    private String description;

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(int amountCents) {
        this.amountCents = amountCents;
    }

    public String getAmountLabel() {
        return amountLabel;
    }

    public void setAmountLabel(String amountLabel) {
        this.amountLabel = amountLabel;
    }

    public int getFullPriceCents() {
        return fullPriceCents;
    }

    public void setFullPriceCents(int fullPriceCents) {
        this.fullPriceCents = fullPriceCents;
    }

    public String getFullPriceLabel() {
        return fullPriceLabel;
    }

    public void setFullPriceLabel(String fullPriceLabel) {
        this.fullPriceLabel = fullPriceLabel;
    }

    public int getMonthlyPriceCents() {
        return monthlyPriceCents;
    }

    public void setMonthlyPriceCents(int monthlyPriceCents) {
        this.monthlyPriceCents = monthlyPriceCents;
    }

    public int getAnnualPriceCents() {
        return annualPriceCents;
    }

    public void setAnnualPriceCents(int annualPriceCents) {
        this.annualPriceCents = annualPriceCents;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public String getInstallmentAmountLabel() {
        return installmentAmountLabel;
    }

    public void setInstallmentAmountLabel(String installmentAmountLabel) {
        this.installmentAmountLabel = installmentAmountLabel;
    }

    public boolean isUpgrade() {
        return upgrade;
    }

    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
