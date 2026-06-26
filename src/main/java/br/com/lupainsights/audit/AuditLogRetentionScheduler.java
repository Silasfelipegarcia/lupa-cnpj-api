package br.com.lupainsights.audit;

import br.com.lupainsights.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AuditLogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionScheduler.class);
    private static final int RETENTION_DAYS = 90;

    private final AuditLogRepository repository;

    public AuditLogRetentionScheduler(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgarLogsAntigos() {
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int removidos = repository.deleteByOccurredAtBefore(cutoff);
        if (removidos > 0) {
            log.info("Audit logs removidos: {} (anteriores a {})", removidos, cutoff);
        }
    }
}
