package br.com.dadoscnpj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String corsAllowedOrigins = "http://localhost:4200";

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String[] getCorsAllowedOriginsArray() {
        return corsAllowedOrigins.split(",");
    }
}
