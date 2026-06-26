package br.com.lupainsights.service;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.dto.ChargePlanRequest;
import br.com.lupainsights.dto.ChargePlanResponse;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.payment.MercadoPagoPaymentService;
import br.com.lupainsights.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TrialService {

    private static final Logger log = LoggerFactory.getLogger(TrialService.class);

    private final UserRepository userRepository;
    private final MercadoPagoPaymentService paymentService;

    public TrialService(UserRepository userRepository,
                        @Lazy MercadoPagoPaymentService paymentService) {
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public UserEntity iniciarTrial(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (user.isTrialUtilizado()) {
            throw new IllegalStateException("Você já utilizou o período de teste gratuito.");
        }

        if (user.getDefaultCardId() == null || user.getDefaultCardId().isBlank()) {
            throw new IllegalStateException(
                    "Cadastre um cartão em Cobrança antes de iniciar o trial de 7 dias.");
        }

        user.setTrialUtilizado(true);
        user.setTrialAte(Instant.now().plus(7, ChronoUnit.DAYS));
        user.setPlan(SubscriptionPlan.PREMIUM);
        user.setAutoRenew(true);
        return userRepository.save(user);
    }

    @Transactional
    public void expirarTrialSeNecessario(UserEntity user) {
        if (user.getTrialAte() == null || !Instant.now().isAfter(user.getTrialAte())) {
            return;
        }
        if (user.getPlan() != SubscriptionPlan.PREMIUM) {
            user.setTrialAte(null);
            userRepository.save(user);
            return;
        }
        if (user.getPlanValidUntil() != null && Instant.now().isBefore(user.getPlanValidUntil())) {
            user.setTrialAte(null);
            userRepository.save(user);
            return;
        }

        if (tentarCobrarAposTrial(user)) {
            user.setTrialAte(null);
            userRepository.save(user);
            return;
        }

        user.setPlan(SubscriptionPlan.FREE);
        user.setTrialAte(null);
        user.setAutoRenew(false);
        userRepository.save(user);
        log.info("Trial expirado sem cobrança para {}", user.getEmail());
    }

    public boolean trialDisponivel(UserEntity user) {
        return !user.isTrialUtilizado() && user.getTrialAte() == null;
    }

    private boolean tentarCobrarAposTrial(UserEntity user) {
        if (!user.isAutoRenew()
                || user.getDefaultCardId() == null
                || user.getDefaultCardId().isBlank()) {
            return false;
        }
        try {
            ChargePlanRequest request = new ChargePlanRequest();
            request.setPlan(SubscriptionPlan.PREMIUM);
            request.setCardId(user.getDefaultCardId());
            request.setRenewal(true);
            ChargePlanResponse response = paymentService.cobrarPlano(user.getId(), request);
            boolean aprovado = "APPROVED".equalsIgnoreCase(response.getStatus());
            if (aprovado) {
                log.info("Trial convertido em assinatura paga para {}", user.getEmail());
            }
            return aprovado;
        } catch (Exception e) {
            log.warn("Cobrança pós-trial falhou para {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }
}
