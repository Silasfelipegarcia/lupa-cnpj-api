package br.com.dadoscnpj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    private String accessToken = "";
    private String frontendUrl = "https://lupa-cnpj.vercel.app";
    private String apiPublicUrl = "https://lupa-cnpj-api-production.up.railway.app";
    private String apiBaseUrl = "https://api.mercadopago.com";
    private int premiumPriceCents = 4990;
    private int proPlusPriceCents = 9990;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public String getApiPublicUrl() {
        return apiPublicUrl;
    }

    public void setApiPublicUrl(String apiPublicUrl) {
        this.apiPublicUrl = apiPublicUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public int getPremiumPriceCents() {
        return premiumPriceCents;
    }

    public void setPremiumPriceCents(int premiumPriceCents) {
        this.premiumPriceCents = premiumPriceCents;
    }

    public int getProPlusPriceCents() {
        return proPlusPriceCents;
    }

    public void setProPlusPriceCents(int proPlusPriceCents) {
        this.proPlusPriceCents = proPlusPriceCents;
    }

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }
}
