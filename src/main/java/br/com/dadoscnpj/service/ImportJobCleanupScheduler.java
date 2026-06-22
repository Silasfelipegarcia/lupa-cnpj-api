package br.com.dadoscnpj.service;

import br.com.dadoscnpj.config.SecurityProperties;
import br.com.dadoscnpj.util.IpRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ImportJobCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImportJobCleanupScheduler.class);
    private static final long HOUR_MS = 60L * 60 * 1000;

    private final ImportJobStore jobStore;
    private final SecurityProperties securityProperties;
    private final IpRateLimiter rateLimiter;

    public ImportJobCleanupScheduler(ImportJobStore jobStore,
                                     SecurityProperties securityProperties,
                                     IpRateLimiter rateLimiter) {
        this.jobStore = jobStore;
        this.securityProperties = securityProperties;
        this.rateLimiter = rateLimiter;
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void limparJobsExpirados() {
        Duration ttl = Duration.ofHours(securityProperties.getJobTtlHours());
        var removidos = jobStore.removerExpirados(ttl);
        if (!removidos.isEmpty()) {
            log.info("Removidos {} job(s) expirado(s) da memória", removidos.size());
        }
        rateLimiter.limparExpirados(HOUR_MS);
    }
}
