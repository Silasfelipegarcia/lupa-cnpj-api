package br.com.lupainsights.plan;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.dto.PlanCatalogItemResponse;
import br.com.lupainsights.dto.PlanUsageResponse;
import br.com.lupainsights.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanService {

    private final PlanLimitsService planLimitsService;
    private final UsageTrackingService usageTrackingService;
    private final br.com.lupainsights.config.MercadoPagoProperties mercadoPagoProperties;

    public PlanService(PlanLimitsService planLimitsService,
                       UsageTrackingService usageTrackingService,
                       br.com.lupainsights.config.MercadoPagoProperties mercadoPagoProperties) {
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
        response.setPesquisaRazaoSocial(limits.pesquisaRazaoSocial());
        response.setExportExcel(limits.exportExcel());
        response.setFiltroSomenteAtivos(limits.filtroSomenteAtivos());
        response.setFiltrosAvancados(limits.filtrosAvancados());
        response.setDedupeHabilitado(limits.dedupeHabilitado());
        response.setTrialDisponivel(!user.isTrialUtilizado() && user.getTrialAte() == null);
        response.setDadosLimitados(limits.dadosLimitados());
        response.setMaxImportJobsPerDay(limits.maxImportJobsPerDay());
        response.setImportJobsToday(snapshot.importJobsToday());
        return response;
    }

    public List<PlanCatalogItemResponse> catalogo() {
        List<PlanCatalogItemResponse> itens = new ArrayList<>();
        itens.add(item(SubscriptionPlan.FREE, 0));
        itens.add(item(SubscriptionPlan.PREMIUM, mercadoPagoProperties.getPremiumPriceCents()));
        itens.add(item(SubscriptionPlan.PRO_PLUS, mercadoPagoProperties.getProPlusPriceCents()));
        itens.add(itemBusiness());
        return itens;
    }

    private PlanCatalogItemResponse item(SubscriptionPlan plan, int priceCents) {
        PlanLimits limits = planLimitsService.limitesDe(planoTemporario(plan));
        PlanCatalogItemResponse item = new PlanCatalogItemResponse();
        item.setPlan(plan);
        item.setNome(planLimitsService.nomeExibicao(plan));
        item.setDescricao(planLimitsService.descricaoCurta(plan));
        item.setMaxRowsPerFile(limits.maxRowsPerFile());
        item.setBatchSearchesPerDay(formatarLimitePlanilhas(limits));
        item.setDirectCnpjPerDay(formatarLimiteDirect(plan, limits));
        item.setPriceCents(priceCents);
        item.setPriceLabel(priceCents == 0 ? "Grátis" : String.format("R$ %.2f/mês", priceCents / 100.0));
        item.setBeneficios(beneficiosDe(plan, limits));
        item.setContatoComercial(false);
        return item;
    }

    private PlanCatalogItemResponse itemBusiness() {
        PlanCatalogItemResponse item = new PlanCatalogItemResponse();
        item.setNome("Business");
        item.setDescricao("Alto volume, API e integrações");
        item.setMaxRowsPerFile(0);
        item.setBatchSearchesPerDay("Sob medida");
        item.setDirectCnpjPerDay("Sob medida");
        item.setPriceCents(0);
        item.setPriceLabel("Fale conosco");
        item.setBeneficios(List.of(
                "API dedicada e webhooks",
                "Integrações com CRM",
                "Volume e SLA customizados",
                "Faturamento para empresas"
        ));
        item.setContatoComercial(true);
        return item;
    }

    private List<String> beneficiosDe(SubscriptionPlan plan, PlanLimits limits) {
        List<String> beneficios = new ArrayList<>();
        beneficios.add("Até " + limits.maxRowsPerFile() + " empresas por planilha");
        beneficios.add(formatarLimitePlanilhas(limits));
        beneficios.add("CNPJ único: " + formatarLimiteDirect(plan, limits) + " por dia");
        if (limits.pesquisaRazaoSocial()) {
            beneficios.add("Busca por razão social");
        }
        if (limits.exportExcel()) {
            beneficios.add("Exportação Excel (.xlsx)");
        }
        if (limits.filtroSomenteAtivos()) {
            beneficios.add("Filtro de empresas ativas");
        }
        if (limits.filtrosAvancados()) {
            beneficios.add("Filtros por UF, CNAE e contato");
        }
        if (limits.dedupeHabilitado()) {
            beneficios.add("Remoção de CNPJs duplicados");
        }
        if (plan == SubscriptionPlan.FREE) {
            beneficios.add("Histórico de 7 dias");
            beneficios.add("Telefone, e-mail e endereço no plano pago");
        } else if (plan == SubscriptionPlan.PREMIUM) {
            beneficios.add("Histórico de 90 dias");
            beneficios.add("7 dias grátis com cartão cadastrado");
        } else if (plan == SubscriptionPlan.PRO_PLUS) {
            beneficios.add("Histórico ilimitado");
        }
        return beneficios;
    }

    private UserEntity planoTemporario(SubscriptionPlan plan) {
        UserEntity user = new UserEntity();
        user.setPlan(plan);
        user.setRole(br.com.lupainsights.domain.UserRole.USER);
        return user;
    }

    private String formatarLimitePlanilhas(PlanLimits limits) {
        if (limits.maxImportJobsPerDay() == null) {
            return "Planilhas/dia: ilimitado";
        }
        return limits.maxImportJobsPerDay() + " planilhas/dia";
    }

    private String formatarLimiteDirect(SubscriptionPlan plan, PlanLimits limits) {
        if (limits.isUnlimitedDirect()) {
            return "Ilimitado";
        }
        return limits.maxDirectCnpjPerDay() + " únicos/dia";
    }
}
