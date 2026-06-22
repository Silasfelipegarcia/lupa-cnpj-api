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
            return new PlanLimits(Integer.MAX_VALUE, null, null);
        }
        return switch (user.getPlan()) {
            case FREE -> new PlanLimits(10, 1, 5);
            case PREMIUM -> new PlanLimits(100, 15, null);
            case PRO_PLUS -> new PlanLimits(900, null, null);
        };
    }

    public String nomeExibicao(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> "Free";
            case PREMIUM -> "Premium";
            case PRO_PLUS -> "Pro+";
        };
    }
}
