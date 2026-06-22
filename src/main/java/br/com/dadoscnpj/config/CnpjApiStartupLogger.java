package br.com.dadoscnpj.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CnpjApiStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(CnpjApiStartupLogger.class);

    private final CnpjApiProperties properties;

    public CnpjApiStartupLogger(CnpjApiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void logarConfiguracao() {
        if (properties.isPesquisaHabilitada()) {
            log.info("Busca por razão social habilitada (CNPJ.ws comercial)");
        } else {
            log.warn("Busca por razão social DESABILITADA. Configure CNPJ_WS_TOKEN no Railway para usar fallback por nome.");
        }
    }
}
