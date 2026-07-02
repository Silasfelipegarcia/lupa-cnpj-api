package br.com.lupainsights.service;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.ChargePlanRequest;
import br.com.lupainsights.dto.ChargePlanResponse;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.payment.MercadoPagoPaymentService;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.subscription.SubscriptionService;
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
    private static final int TRIAL_DIAS = 7;

    private final UserRepository userRepository;
    private final MercadoPagoPaymentService paymentService;
    private final SubscriptionService subscriptionService;

    public TrialService(UserRepository userRepository,
                        @Lazy MercadoPagoPaymentService paymentService,
                        SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.subscriptionService = subscriptionService;
    }

    public boolean emTrial(UserEntity user) {
        return user.getTrialAte() != null
                && Instant.now().isBefore(user.getTrialAte())
                && user.getPlan() == SubscriptionPlan.PREMIUM
                && !subscriptionService.temAssinaturaPaga(user);
    }

    public int diasRestantesTrial(UserEntity user) {
        if (!emTrial(user)) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(Instant.now(), user.getTrialAte()));
    }

    @Transactional
    public void ativarTrialInicialSeElegivel(UserEntity user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!user.isEmailVerified()) {
            return;
        }
        if (user.isTrialUtilizado()) {
            return;
        }
        if (subscriptionService.temAssinaturaPaga(user)) {
            return;
        }
        if (emTrial(user)) {
            return;
        }

        user.setTrialUtilizado(true);
        user.setTrialAte(Instant.now().plus(TRIAL_DIAS, ChronoUnit.DAYS));
        user.setPlan(SubscriptionPlan.PREMIUM);
        user.setAutoRenew(false);
        userRepository.save(user);
        log.info("Trial de {} dias ativado para {}", TRIAL_DIAS, user.getEmail());
    }

    /**
     * Confirma conversão automática ao fim do trial (requer cartão salvo).
     */
    @Transactional
    public UserEntity confirmarConversaoTrial(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!emTrial(user)) {
            throw new IllegalStateException("Seu trial não está ativo no momento.");
        }

        if (user.getDefaultCardId() == null || user.getDefaultCardId().isBlank()) {
            throw new IllegalStateException(
                    "Cadastre um cartão em Cobrança para continuar no Prospecção após o trial.");
        }

        user.setAutoRenew(true);
        return userRepository.save(user);
    }

    @Transactional
    public void habilitarConversaoAoSalvarCartao(UserEntity user) {
        if (emTrial(user) && !user.isAutoRenew()) {
            user.setAutoRenew(true);
            userRepository.save(user);
            log.info("Conversão automática habilitada no trial para {}", user.getEmail());
        }
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
        if (subscriptionService.temAssinaturaPaga(user)) {
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
        return false;
    }

    public boolean conversaoTrialPendente(UserEntity user) {
        return emTrial(user) && !user.isAutoRenew();
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
            request.setInstallments(1);
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
