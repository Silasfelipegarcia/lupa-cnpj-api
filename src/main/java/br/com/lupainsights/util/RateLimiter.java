package br.com.lupainsights.util;

import br.com.lupainsights.config.CnpjApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final CnpjApiProperties properties;
    private long lastRequestTime = 0;

    public RateLimiter(CnpjApiProperties properties) {
        this.properties = properties;
        long minDelayMs = properties.getMinDelayBetweenRequestsMs();
        log.info("Rate limiter configurado: {} ms entre consultas ({} req/min)",
                minDelayMs, rpmAtual());
    }

    private int rpmAtual() {
        return properties.isConsultaComercialAtiva()
                ? properties.getCommercialRateLimitPerMinute()
                : properties.getRateLimitPerMinute();
    }

    public synchronized void acquire() throws InterruptedException {
        long minDelayMs = properties.getMinDelayBetweenRequestsMs();
        long now = System.currentTimeMillis();
        if (lastRequestTime > 0) {
            long elapsed = now - lastRequestTime;
            if (elapsed < minDelayMs) {
                long waitMs = minDelayMs - elapsed;
                log.debug("Aguardando {} ms para respeitar limite de consultas", waitMs);
                Thread.sleep(waitMs);
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
