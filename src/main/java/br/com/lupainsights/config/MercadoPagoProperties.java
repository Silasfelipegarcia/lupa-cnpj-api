package br.com.lupainsights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    private String accessToken = "";
    private String publicKey = "";
    private String frontendUrl = "https://lupa-insights.vercel.app";
    private String apiPublicUrl = "https://lupa-cnpj-api-production.up.railway.app";
    private String apiBaseUrl = "https://api.mercadopago.com";
    private int premiumPriceCents = 4990;
    private int proPlusPriceCents = 9990;
    private String webhookSecret = "";

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
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

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    public boolean isCheckoutReady() {
        return isConfigured() && publicKey != null && !publicKey.isBlank();
    }
}
