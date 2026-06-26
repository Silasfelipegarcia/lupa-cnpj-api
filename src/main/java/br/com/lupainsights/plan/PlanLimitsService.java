package br.com.lupainsights.plan;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.subscription.SubscriptionService;
import org.springframework.stereotype.Service;

@Service
public class PlanLimitsService {

    private final SubscriptionService subscriptionService;

    public PlanLimitsService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public boolean isMaster(UserEntity user) {
        return user.getRole() == UserRole.ADMIN;
    }

    public PlanLimits limitesDe(UserEntity user) {
        if (isMaster(user)) {
            return new PlanLimits(
                    Integer.MAX_VALUE, null, null,
                    true, true, true, true, true);
        }
        SubscriptionPlan plan = subscriptionService.resolverPlanoEfetivo(user);
        return switch (plan) {
            case FREE -> new PlanLimits(10, 10, 15, false, false, false, false, false);
            case PREMIUM -> new PlanLimits(100, 100, null, true, true, true, false, false);
            case PRO_PLUS -> new PlanLimits(900, 500, null, true, true, true, true, true);
        };
    }

    public String nomeExibicao(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> "Free";
            case PREMIUM -> "Prospecção";
            case PRO_PLUS -> "Growth";
        };
    }

    public String descricaoCurta(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> "Para testar com listas pequenas";
            case PREMIUM -> "SDRs e pré-vendas no dia a dia";
            case PRO_PLUS -> "Volume e filtros para equipes comerciais";
        };
    }
}
