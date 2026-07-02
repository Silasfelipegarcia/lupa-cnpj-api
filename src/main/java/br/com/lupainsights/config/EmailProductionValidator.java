package br.com.lupainsights.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class EmailProductionValidator {

    private static final Logger log = LoggerFactory.getLogger(EmailProductionValidator.class);

    private final Environment environment;
    private final EmailProperties emailProperties;

    public EmailProductionValidator(Environment environment, EmailProperties emailProperties) {
        this.environment = environment;
        this.emailProperties = emailProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validarProducao() {
        if (!isProduction()) {
            return;
        }
        if (emailProperties.isEnabled() && !emailProperties.isResendConfigured()) {
            throw new IllegalStateException(
                    "Produção: RESEND_API_KEY é obrigatória quando EMAIL_ENABLED=true");
        }
        if (emailProperties.isEnabled()) {
            log.info(
                    "E-mail transacional ativo: from={}, frontendUrl={}",
                    emailProperties.formatFromAddress(),
                    emailProperties.getFrontendUrl());
        }
    }

    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("production");
    }
}
