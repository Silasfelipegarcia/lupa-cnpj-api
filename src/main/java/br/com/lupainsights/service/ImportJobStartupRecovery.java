package br.com.lupainsights.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ImportJobStartupRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportJobStartupRecovery.class);

    private final ImportJobQueueService jobQueueService;

    public ImportJobStartupRecovery(ImportJobQueueService jobQueueService) {
        this.jobQueueService = jobQueueService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int recuperados = jobQueueService.recuperarJobsPendentes();
        if (recuperados > 0) {
            log.info("Recuperados {} job(s) pendente(s) do banco após inicialização", recuperados);
        }
    }
}
