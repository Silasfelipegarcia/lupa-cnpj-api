package br.com.dadoscnpj.dto;

import br.com.dadoscnpj.domain.SubscriptionPlan;

public class PlanCatalogItemResponse {

    private SubscriptionPlan plan;
    private String nome;
    private int maxRowsPerFile;
    private String batchSearchesPerDay;
    private String directCnpjPerDay;
    private int priceCents;
    private String priceLabel;

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getMaxRowsPerFile() {
        return maxRowsPerFile;
    }

    public void setMaxRowsPerFile(int maxRowsPerFile) {
        this.maxRowsPerFile = maxRowsPerFile;
    }

    public String getBatchSearchesPerDay() {
        return batchSearchesPerDay;
    }

    public void setBatchSearchesPerDay(String batchSearchesPerDay) {
        this.batchSearchesPerDay = batchSearchesPerDay;
    }

    public String getDirectCnpjPerDay() {
        return directCnpjPerDay;
    }

    public void setDirectCnpjPerDay(String directCnpjPerDay) {
        this.directCnpjPerDay = directCnpjPerDay;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(int priceCents) {
        this.priceCents = priceCents;
    }

    public String getPriceLabel() {
        return priceLabel;
    }

    public void setPriceLabel(String priceLabel) {
        this.priceLabel = priceLabel;
    }
}
