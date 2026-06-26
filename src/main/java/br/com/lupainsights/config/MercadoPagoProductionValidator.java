package br.com.lupainsights.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MercadoPagoProductionValidator {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoProductionValidator.class);

    private final Environment environment;
    private final MercadoPagoProperties properties;

    public MercadoPagoProductionValidator(Environment environment, MercadoPagoProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validarProducao() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("production")) {
            return;
        }
        if (!properties.isConfigured()) {
            log.warn("Produção: MERCADOPAGO_ACCESS_TOKEN ausente — pagamentos desabilitados.");
            return;
        }
        String token = properties.getAccessToken();
        if (token.startsWith("TEST-")) {
            log.error("Produção: MERCADOPAGO_ACCESS_TOKEN usa prefixo TEST-. Use credenciais APP_USR- no Railway.");
        }
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            log.error("Produção: MERCADOPAGO_WEBHOOK_SECRET ausente — webhooks serão rejeitados (fail-closed).");
        }
        String apiUrl = properties.getApiPublicUrl();
        if (apiUrl.contains("localhost") || apiUrl.contains("127.0.0.1")) {
            log.error("Produção: API_PUBLIC_URL aponta para localhost ({})", apiUrl);
        }
        String frontend = properties.getFrontendUrl();
        if (frontend.contains("localhost") || frontend.contains("127.0.0.1")) {
            log.error("Produção: FRONTEND_URL aponta para localhost ({})", frontend);
        }
    }
}
