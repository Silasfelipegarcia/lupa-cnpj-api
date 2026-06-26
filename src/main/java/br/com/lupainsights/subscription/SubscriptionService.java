package br.com.lupainsights.subscription;

import br.com.lupainsights.config.MercadoPagoProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.SubscriptionStatus;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.PlanQuoteResponse;
import br.com.lupainsights.dto.SubscriptionStatusResponse;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class SubscriptionService {

    private static final int PERIODO_DIAS = 30;

    private final UserRepository userRepository;
    private final MercadoPagoProperties properties;

    public SubscriptionService(UserRepository userRepository, MercadoPagoProperties properties) {
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional
    public void ativarPeriodoPago(UserEntity user, SubscriptionPlan plan, Instant paidAt, boolean renewal) {
        Instant base = user.getPlanValidUntil() != null && user.getPlanValidUntil().isAfter(paidAt)
                ? user.getPlanValidUntil()
                : paidAt;

        user.setPlan(plan);
        user.setPlanValidUntil(base.plus(PERIODO_DIAS, ChronoUnit.DAYS));
        user.setAutoRenew(true);
        user.setPlanCancelledAt(null);
        user.setTrialAte(null);
        userRepository.save(user);
    }

    @Transactional
    public void aplicarUpgrade(UserEntity user, SubscriptionPlan novoPlano) {
        user.setPlan(novoPlano);
        user.setAutoRenew(true);
        user.setPlanCancelledAt(null);
        user.setTrialAte(null);
        userRepository.save(user);
    }

    public boolean ehUpgradeProporcional(UserEntity user, SubscriptionPlan targetPlan) {
        return targetPlan == SubscriptionPlan.PRO_PLUS
                && resolverPlanoEfetivo(user) == SubscriptionPlan.PREMIUM
                && temAssinaturaPaga(user);
    }

    public int calcularValorCobranca(UserEntity user, SubscriptionPlan targetPlan) {
        int precoCheio = precoPlano(targetPlan);
        if (ehUpgradeProporcional(user, targetPlan)) {
            int diff = properties.getProPlusPriceCents() - properties.getPremiumPriceCents();
            long dias = ChronoUnit.DAYS.between(Instant.now(), user.getPlanValidUntil());
            dias = Math.max(1, Math.min(dias, PERIODO_DIAS));
            int proporcional = (int) Math.round(diff * (dias / (double) PERIODO_DIAS));
            return Math.max(100, proporcional);
        }
        return precoCheio;
    }

    public PlanQuoteResponse montarCotacao(UserEntity user, SubscriptionPlan targetPlan) {
        if (targetPlan != SubscriptionPlan.PREMIUM && targetPlan != SubscriptionPlan.PRO_PLUS) {
            throw new IllegalArgumentException("Plano inválido para cotação");
        }

        int fullPrice = precoPlano(targetPlan);
        int amount = calcularValorCobranca(user, targetPlan);
        boolean upgrade = ehUpgradeProporcional(user, targetPlan);

        PlanQuoteResponse quote = new PlanQuoteResponse();
        quote.setPlan(targetPlan);
        quote.setAmountCents(amount);
        quote.setAmountLabel(formatarValor(amount));
        quote.setFullPriceCents(fullPrice);
        quote.setFullPriceLabel(formatarValor(fullPrice));
        quote.setUpgrade(upgrade);
        if (upgrade && user.getPlanValidUntil() != null) {
            long dias = ChronoUnit.DAYS.between(Instant.now(), user.getPlanValidUntil());
            quote.setDescription("Diferença proporcional aos " + Math.max(1, dias) + " dias restantes do período");
        }
        return quote;
    }

    private int precoPlano(SubscriptionPlan plan) {
        return plan == SubscriptionPlan.PREMIUM
                ? properties.getPremiumPriceCents()
                : properties.getProPlusPriceCents();
    }

    private String formatarValor(int amountCents) {
        double valor = amountCents / 100.0;
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valor);
    }

    public void definirCartaoPadrao(UserEntity user, String cardId) {
        if (cardId != null && !cardId.isBlank()) {
            user.setDefaultCardId(cardId);
            userRepository.save(user);
        }
    }

    @Transactional
    public UserEntity cancelar(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!temAssinaturaPaga(user)) {
            throw new IllegalStateException("Você não possui uma assinatura ativa para cancelar.");
        }
        if (user.getPlanCancelledAt() != null) {
            throw new IllegalStateException("A renovação automática já está cancelada.");
        }

        user.setAutoRenew(false);
        user.setPlanCancelledAt(Instant.now());
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity reativar(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (user.getPlanCancelledAt() == null) {
            throw new IllegalStateException("Não há cancelamento pendente para reativar.");
        }
        if (!periodoAindaValido(user)) {
            throw new IllegalStateException("O período da assinatura já expirou. Assine um plano novamente.");
        }

        user.setAutoRenew(true);
        user.setPlanCancelledAt(null);
        return userRepository.save(user);
    }

    @Transactional
    public void expirarSeNecessario(UserEntity user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getPlanValidUntil() == null) {
            return;
        }
        if (Instant.now().isAfter(user.getPlanValidUntil()) && isPlanoPago(user.getPlan())) {
            user.setPlan(SubscriptionPlan.FREE);
            user.setPlanValidUntil(null);
            user.setPlanCancelledAt(null);
            user.setAutoRenew(false);
            user.setDefaultCardId(null);
            userRepository.save(user);
        }
    }

    public SubscriptionPlan resolverPlanoEfetivo(UserEntity user) {
        if (user.getRole() == UserRole.ADMIN) {
            return user.getPlan();
        }
        if (!isPlanoPago(user.getPlan())) {
            return user.getPlan();
        }
        if (user.getPlanValidUntil() != null && Instant.now().isAfter(user.getPlanValidUntil())) {
            return SubscriptionPlan.FREE;
        }
        return user.getPlan();
    }

    public SubscriptionStatus statusAssinatura(UserEntity user) {
        if (!isPlanoPago(user.getPlan()) || user.getPlanValidUntil() == null) {
            if (user.getPlanValidUntil() != null
                    && Instant.now().isAfter(user.getPlanValidUntil())
                    && isPlanoPago(user.getPlan())) {
                return SubscriptionStatus.EXPIRED;
            }
            return SubscriptionStatus.NONE;
        }
        if (Instant.now().isAfter(user.getPlanValidUntil())) {
            return SubscriptionStatus.EXPIRED;
        }
        if (user.getPlanCancelledAt() != null || !user.isAutoRenew()) {
            return SubscriptionStatus.CANCELLED_PENDING;
        }
        return SubscriptionStatus.ACTIVE;
    }

    public SubscriptionStatusResponse montarStatus(UserEntity user) {
        SubscriptionStatusResponse response = new SubscriptionStatusResponse();
        SubscriptionStatus status = statusAssinatura(user);
        response.setStatus(status.name());
        response.setAutoRenew(user.isAutoRenew());
        response.setValidUntil(user.getPlanValidUntil());
        response.setCancelledAt(user.getPlanCancelledAt());
        response.setDefaultCardId(user.getDefaultCardId());
        response.setPlan(user.getPlan());
        response.setPlanNome(planoNome(user));

        if (user.getPlanValidUntil() != null && periodoAindaValido(user)) {
            long dias = ChronoUnit.DAYS.between(Instant.now(), user.getPlanValidUntil());
            response.setDaysRemaining(Math.max(0, dias));
        } else {
            response.setDaysRemaining(0);
        }

        response.setPodeCancelar(status == SubscriptionStatus.ACTIVE);
        response.setPodeReativar(status == SubscriptionStatus.CANCELLED_PENDING);
        return response;
    }

    public boolean temAssinaturaPaga(UserEntity user) {
        return isPlanoPago(user.getPlan()) && periodoAindaValido(user);
    }

    public boolean periodoAindaValido(UserEntity user) {
        return user.getPlanValidUntil() != null && !Instant.now().isAfter(user.getPlanValidUntil());
    }

    private boolean isPlanoPago(SubscriptionPlan plan) {
        return plan == SubscriptionPlan.PREMIUM || plan == SubscriptionPlan.PRO_PLUS;
    }

    private String planoNome(UserEntity user) {
        return switch (user.getPlan()) {
            case FREE -> "Free";
            case PREMIUM -> "Prospecção";
            case PRO_PLUS -> "Growth";
        };
    }
}
