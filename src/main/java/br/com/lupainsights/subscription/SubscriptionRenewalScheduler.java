package br.com.lupainsights.subscription;

import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.payment.MercadoPagoPaymentService;
import br.com.lupainsights.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SubscriptionRenewalScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRenewalScheduler.class);
    private static final int DIAS_ANTECEDENCIA = 3;

    private final UserRepository userRepository;
    private final MercadoPagoPaymentService paymentService;

    public SubscriptionRenewalScheduler(UserRepository userRepository,
                                        MercadoPagoPaymentService paymentService) {
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void processarRenovacoes() {
        Instant limite = Instant.now().plus(DIAS_ANTECEDENCIA, ChronoUnit.DAYS);
        List<UserEntity> candidatos = userRepository.findDueForRenewal(limite);
        if (candidatos.isEmpty()) {
            return;
        }
        log.info("Processando renovação automática para {} assinatura(s)", candidatos.size());
        for (UserEntity user : candidatos) {
            paymentService.cobrarRenovacaoAutomatica(user);
        }
    }
}
