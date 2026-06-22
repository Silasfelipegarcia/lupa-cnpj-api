package br.com.dadoscnpj.plan;

import br.com.dadoscnpj.domain.SubscriptionPlan;
import br.com.dadoscnpj.domain.UserRole;
import br.com.dadoscnpj.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class PlanLimitsService {

    public boolean isMaster(UserEntity user) {
        return user.getRole() == UserRole.ADMIN;
    }

    public PlanLimits limitesDe(UserEntity user) {
        if (isMaster(user)) {
            return new PlanLimits(
                    Integer.MAX_VALUE, null, null,
                    true, true, true, true, true);
        }
        return switch (user.getPlan()) {
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
