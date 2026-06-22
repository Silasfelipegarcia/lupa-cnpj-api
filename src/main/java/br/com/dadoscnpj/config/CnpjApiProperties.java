package br.com.dadoscnpj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cnpj.api")
public class CnpjApiProperties {

    private String baseUrl = "https://publica.cnpj.ws/cnpj";
    private String pesquisaUrl = "https://comercial.cnpj.ws/v2/pesquisa";
    private String token = "";
    private int rateLimitPerMinute = 3;
    private int timeoutSeconds = 30;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPesquisaUrl() {
        return pesquisaUrl;
    }

    public void setPesquisaUrl(String pesquisaUrl) {
        this.pesquisaUrl = pesquisaUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isPesquisaHabilitada() {
        return token != null && !token.isBlank();
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public long getMinDelayBetweenRequestsMs() {
        return 60_000L / Math.max(rateLimitPerMinute, 1);
    }
}
