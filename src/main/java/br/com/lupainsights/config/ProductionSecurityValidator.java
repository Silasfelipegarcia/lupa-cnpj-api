package br.com.lupainsights.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ProductionSecurityValidator {

    private static final String DEV_JWT_SECRET = "dev-secret-change-in-production-min-32-chars";

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final MercadoPagoProperties mercadoPagoProperties;

    public ProductionSecurityValidator(Environment environment,
                                     JwtProperties jwtProperties,
                                     MercadoPagoProperties mercadoPagoProperties) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.mercadoPagoProperties = mercadoPagoProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validarProducao() {
        if (!isProduction()) {
            return;
        }

        String jwtSecret = jwtProperties.getSecret();
        if (jwtSecret == null || jwtSecret.isBlank() || DEV_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "Produção: defina JWT_SECRET com valor forte (mín. 32 caracteres). Secret default/dev não é permitido.");
        }

        if (mercadoPagoProperties.isConfigured()
                && (mercadoPagoProperties.getWebhookSecret() == null
                || mercadoPagoProperties.getWebhookSecret().isBlank())) {
            throw new IllegalStateException(
                    "Produção: MERCADOPAGO_WEBHOOK_SECRET é obrigatório quando pagamentos estão configurados.");
        }
    }

    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("production");
    }
}
