package br.com.lupainsights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String corsAllowedOrigins = "http://localhost:4200,https://www.lupacnpjs.com.br,https://lupacnpjs.com.br,https://lupa-cnpj.vercel.app,https://*.vercel.app";

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String[] getCorsAllowedOriginsArray() {
        return java.util.Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }
}
