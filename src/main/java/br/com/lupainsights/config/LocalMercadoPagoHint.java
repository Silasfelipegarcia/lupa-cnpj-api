package br.com.lupainsights.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Component
public class LocalMercadoPagoHint {

    private static final Logger log = LoggerFactory.getLogger(LocalMercadoPagoHint.class);

    private final Environment environment;
    private final MercadoPagoProperties properties;

    public LocalMercadoPagoHint(Environment environment, MercadoPagoProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void avisarSeMpAusenteEmDev() {
        if (isProduction() || properties.isCheckoutReady()) {
            return;
        }

        boolean envFileExists = Files.exists(Path.of(".env"));
        if (envFileExists) {
            log.warn("""
                    Mercado Pago não configurado. O arquivo .env existe, mas MERCADOPAGO_PUBLIC_KEY \
                    e/ou MERCADOPAGO_ACCESS_TOKEN estão vazios ou inválidos. Confira o painel MP (credenciais TEST).""");
        } else {
            log.warn("""
                    Mercado Pago não configurado. Crie .env na raiz da API: cp .env.example .env \
                    (ou use ./scripts/start-local.sh). Sem isso, checkout e cartão ficam desabilitados.""");
        }
    }

    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("production");
    }
}
