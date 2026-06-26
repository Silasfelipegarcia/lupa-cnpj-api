package br.com.lupainsights.service;

import br.com.lupainsights.payment.IdempotencyService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyCleanupScheduler {

    private final IdempotencyService idempotencyService;

    public IdempotencyCleanupScheduler(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(cron = "0 30 4 * * *")
    public void limparChavesExpiradas() {
        idempotencyService.limparExpirados();
    }
}
