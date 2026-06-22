package br.com.dadoscnpj.plan;

import br.com.dadoscnpj.domain.SubscriptionPlan;
import br.com.dadoscnpj.dto.PlanCatalogItemResponse;
import br.com.dadoscnpj.dto.PlanUsageResponse;
import br.com.dadoscnpj.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanService {

    private final PlanLimitsService planLimitsService;
    private final UsageTrackingService usageTrackingService;
    private final br.com.dadoscnpj.config.MercadoPagoProperties mercadoPagoProperties;

    public PlanService(PlanLimitsService planLimitsService,
                       UsageTrackingService usageTrackingService,
                       br.com.dadoscnpj.config.MercadoPagoProperties mercadoPagoProperties) {
        this.planLimitsService = planLimitsService;
        this.usageTrackingService = usageTrackingService;
        this.mercadoPagoProperties = mercadoPagoProperties;
    }

    public PlanUsageResponse montarUsage(UserEntity user) {
        UsageSnapshot snapshot = usageTrackingService.snapshot(user.getId());
        PlanLimits limits = snapshot.limits();

        PlanUsageResponse response = new PlanUsageResponse();
        response.setMaxRowsPerFile(limits.maxRowsPerFile());
        response.setMaxBatchSearchesPerDay(limits.maxBatchSearchesPerDay());
        response.setMaxDirectCnpjPerDay(limits.maxDirectCnpjPerDay());
        response.setBatchSearchesToday(snapshot.batchSearchesToday());
        response.setDirectCnpjToday(snapshot.directCnpjToday());
        response.setMaster(snapshot.master());
        return response;
    }

    public List<PlanCatalogItemResponse> catalogo() {
        return List.of(
                item(SubscriptionPlan.FREE, 0),
                item(SubscriptionPlan.PREMIUM, mercadoPagoProperties.getPremiumPriceCents()),
                item(SubscriptionPlan.PRO_PLUS, mercadoPagoProperties.getProPlusPriceCents())
        );
    }

    private PlanCatalogItemResponse item(SubscriptionPlan plan, int priceCents) {
        PlanLimits limits = planLimitsService.limitesDe(planoTemporario(plan));
        PlanCatalogItemResponse item = new PlanCatalogItemResponse();
        item.setPlan(plan);
        item.setNome(planLimitsService.nomeExibicao(plan));
        item.setMaxRowsPerFile(limits.maxRowsPerFile());
        item.setBatchSearchesPerDay(formatarLimite(limits.maxBatchSearchesPerDay()));
        item.setDirectCnpjPerDay(formatarLimite(limits.maxDirectCnpjPerDay()));
        item.setPriceCents(priceCents);
        item.setPriceLabel(priceCents == 0 ? "Grátis" : String.format("R$ %.2f/mês", priceCents / 100.0));
        return item;
    }

    private UserEntity planoTemporario(SubscriptionPlan plan) {
        UserEntity user = new UserEntity();
        user.setPlan(plan);
        user.setRole(br.com.dadoscnpj.domain.UserRole.USER);
        return user;
    }

    private String formatarLimite(Integer valor) {
        return valor == null ? "Ilimitado" : String.valueOf(valor);
    }
}
