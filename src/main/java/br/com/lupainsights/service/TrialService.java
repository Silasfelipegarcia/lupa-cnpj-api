package br.com.lupainsights.service;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TrialService {

    private final UserRepository userRepository;

    public TrialService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity iniciarTrial(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (user.isTrialUtilizado()) {
            throw new IllegalStateException("Você já utilizou o período de teste gratuito.");
        }

        user.setTrialUtilizado(true);
        user.setTrialAte(Instant.now().plus(7, ChronoUnit.DAYS));
        user.setPlan(SubscriptionPlan.PREMIUM);
        return userRepository.save(user);
    }

    @Transactional
    public void expirarTrialSeNecessario(UserEntity user) {
        if (user.getTrialAte() == null) {
            return;
        }
        if (!Instant.now().isAfter(user.getTrialAte()) || user.getPlan() != SubscriptionPlan.PREMIUM) {
            return;
        }
        if (user.getPlanValidUntil() != null && !Instant.now().isAfter(user.getPlanValidUntil())) {
            user.setTrialAte(null);
            userRepository.save(user);
            return;
        }
        user.setPlan(SubscriptionPlan.FREE);
        user.setTrialAte(null);
        userRepository.save(user);
    }

    public boolean trialDisponivel(UserEntity user) {
        return !user.isTrialUtilizado() && user.getTrialAte() == null;
    }
}
