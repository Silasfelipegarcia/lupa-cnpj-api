package br.com.lupainsights.config;

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
        if (properties.isConsultaComercialAtiva()) {
            log.info("Consulta CNPJ via API comercial (token configurado)");
        } else {
            log.info("Consulta CNPJ via API pública (3 req/min)");
        }

        if (properties.isPesquisaRazaoSocialAtiva()) {
            log.info("Busca por razão social habilitada (CNPJ.ws comercial)");
        } else if (properties.isPesquisaRazaoSocialHabilitada()) {
            log.warn("PESQUISA_RAZAO_SOCIAL=true mas CNPJ_WS_TOKEN não configurado. Busca por nome permanece desligada.");
        } else {
            log.info("Busca por razão social desligada. Apenas consultas por CNPJ serão processadas.");
        }
    }
}
