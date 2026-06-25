package br.com.lupainsights.dto;

public class PaymentConfigResponse {

    private String publicKey;
    private boolean configured;

    public PaymentConfigResponse() {
    }

    public PaymentConfigResponse(String publicKey, boolean configured) {
        this.publicKey = publicKey;
        this.configured = configured;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }
}
