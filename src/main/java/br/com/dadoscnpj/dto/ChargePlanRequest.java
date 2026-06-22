package br.com.dadoscnpj.dto;

import br.com.dadoscnpj.domain.SubscriptionPlan;

public class ChargePlanRequest {

    private SubscriptionPlan plan;
    private String cardId;
    private String securityCode;
    /** Token de cartão novo (alternativa a cardId). */
    private String token;

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(String securityCode) {
        this.securityCode = securityCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
