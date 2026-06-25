package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;

import java.util.List;

public class PlanCatalogItemResponse {

    private SubscriptionPlan plan;
    private String nome;
    private String descricao;
    private int maxRowsPerFile;
    private String batchSearchesPerDay;
    private String directCnpjPerDay;
    private int priceCents;
    private String priceLabel;
    private List<String> beneficios;
    private boolean contatoComercial;

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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
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

    public List<String> getBeneficios() {
        return beneficios;
    }

    public void setBeneficios(List<String> beneficios) {
        this.beneficios = beneficios;
    }

    public boolean isContatoComercial() {
        return contatoComercial;
    }

    public void setContatoComercial(boolean contatoComercial) {
        this.contatoComercial = contatoComercial;
    }
}
