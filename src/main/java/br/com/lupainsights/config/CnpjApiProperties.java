package br.com.lupainsights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cnpj.api")
public class CnpjApiProperties {

    private String baseUrl = "https://publica.cnpj.ws/cnpj";
    private String pesquisaUrl = "https://comercial.cnpj.ws/v2/pesquisa";
    private String token = "";
    private boolean pesquisaRazaoSocialHabilitada = false;
    private int rateLimitPerMinute = 3;
    private int commercialRateLimitPerMinute = 60;
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

    public boolean isPesquisaRazaoSocialHabilitada() {
        return pesquisaRazaoSocialHabilitada;
    }

    public void setPesquisaRazaoSocialHabilitada(boolean pesquisaRazaoSocialHabilitada) {
        this.pesquisaRazaoSocialHabilitada = pesquisaRazaoSocialHabilitada;
    }

    public boolean isPesquisaRazaoSocialAtiva() {
        return pesquisaRazaoSocialHabilitada && token != null && !token.isBlank();
    }

    public boolean isPesquisaHabilitada() {
        return token != null && !token.isBlank();
    }

    public boolean isConsultaComercialAtiva() {
        return isPesquisaHabilitada();
    }

    public int getCommercialRateLimitPerMinute() {
        return commercialRateLimitPerMinute;
    }

    public void setCommercialRateLimitPerMinute(int commercialRateLimitPerMinute) {
        this.commercialRateLimitPerMinute = commercialRateLimitPerMinute;
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
        int rpm = isConsultaComercialAtiva() ? commercialRateLimitPerMinute : rateLimitPerMinute;
        return 60_000L / Math.max(rpm, 1);
    }

    public String getConsultaBaseUrl() {
        if (isConsultaComercialAtiva()) {
            return "https://comercial.cnpj.ws/cnpj";
        }
        return normalizarBaseUrlPublica(baseUrl);
    }

    private String normalizarBaseUrlPublica(String url) {
        if (url == null || url.isBlank()) {
            return "https://publica.cnpj.ws/cnpj";
        }
        String trimmed = url.trim().replaceAll("/+$", "");
        if (trimmed.endsWith("/cnpj")) {
            return trimmed;
        }
        if (trimmed.endsWith("publica.cnpj.ws")) {
            return trimmed + "/cnpj";
        }
        return trimmed;
    }
}
