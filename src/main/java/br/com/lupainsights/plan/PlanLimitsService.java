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
                    true, true, true, true, true,
                    false, null);
        }
        SubscriptionPlan plan = subscriptionService.resolverPlanoEfetivo(user);
        return switch (plan) {
            case FREE -> new PlanLimits(5, null, 3, false, false, false, false, false, true, 1);
            case PREMIUM -> new PlanLimits(100, null, null, true, true, true, false, false, false, 10);
            case PRO_PLUS -> new PlanLimits(500, null, null, true, true, true, true, true, false, 50);
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
            case FREE -> "3 CNPJs/dia · 1 planilha de até 5 linhas";
            case PREMIUM -> "10 planilhas/dia · até 100 empresas por planilha";
            case PRO_PLUS -> "50 planilhas/dia · até 500 empresas por planilha";
        };
    }
}
