package br.com.lupainsights.plan;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.dto.PlanCatalogItemResponse;
import br.com.lupainsights.dto.PlanUsageResponse;
import br.com.lupainsights.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PlanService {

    private final PlanLimitsService planLimitsService;
    private final UsageTrackingService usageTrackingService;
    private final br.com.lupainsights.config.MercadoPagoProperties mercadoPagoProperties;
    private final br.com.lupainsights.service.TrialService trialService;

    public PlanService(PlanLimitsService planLimitsService,
                       UsageTrackingService usageTrackingService,
                       br.com.lupainsights.config.MercadoPagoProperties mercadoPagoProperties,
                       br.com.lupainsights.service.TrialService trialService) {
        this.planLimitsService = planLimitsService;
        this.usageTrackingService = usageTrackingService;
        this.mercadoPagoProperties = mercadoPagoProperties;
        this.trialService = trialService;
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
        response.setTrialDisponivel(trialService.trialDisponivel(user));
        response.setEmTrial(trialService.emTrial(user));
        response.setTrialDiasRestantes(trialService.diasRestantesTrial(user));
        response.setConversaoTrialPendente(trialService.conversaoTrialPendente(user));
        response.setDadosLimitados(limits.dadosLimitados());
        response.setMaxImportJobsPerDay(limits.maxImportJobsPerDay());
        response.setImportJobsToday(snapshot.importJobsToday());
        return response;
    }

    public List<PlanCatalogItemResponse> catalogo(boolean incluirPlanoAdmin) {
        List<PlanCatalogItemResponse> itens = new ArrayList<>();
        itens.add(item(SubscriptionPlan.PREMIUM, mercadoPagoProperties.getPremiumPriceCents()));
        itens.add(item(SubscriptionPlan.PRO_PLUS, mercadoPagoProperties.getProPlusPriceCents()));
        if (incluirPlanoAdmin) {
            itens.add(itemAdminTest());
        }
        return itens;
    }

    private PlanCatalogItemResponse itemAdminTest() {
        int priceCents = mercadoPagoProperties.getAdminTestPriceCents();
        PlanCatalogItemResponse item = new PlanCatalogItemResponse();
        item.setPlan(SubscriptionPlan.ADMIN_TEST);
        item.setNome("Teste Admin");
        item.setDescricao("Pagamento único de R$ 1,00 para validar cartão e fluxo de cobrança");
        item.setMaxRowsPerFile(0);
        item.setBatchSearchesPerDay("—");
        item.setDirectCnpjPerDay("—");
        item.setPriceCents(priceCents);
        item.setMonthlyPriceCents(priceCents);
        item.setAnnualPriceCents(priceCents);
        item.setPriceLabel(String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", priceCents / 100.0));
        item.setAnnualPriceLabel("Pagamento único");
        item.setPaymentOptionsLabel("Somente administradores");
        item.setBeneficios(List.of(
                "Cobrança real de R$ 1,00 no cartão",
                "Valida integração com Mercado Pago",
                "Não altera seu acesso Master",
                "Pode repetir quando precisar testar"
        ));
        item.setContatoComercial(false);
        item.setSomenteAdmin(true);
        return item;
    }

    private List<String> beneficiosDe(SubscriptionPlan plan, PlanLimits limits) {
        List<String> beneficios = new ArrayList<>();
        if (plan == SubscriptionPlan.PRO_PLUS) {
            beneficios.add("Tudo do Prospecção");
            beneficios.add("Filtros por UF, CNAE e contato");
            beneficios.add("Remoção de CNPJs duplicados");
            beneficios.add("Histórico ilimitado");
            return beneficios;
        }
        if (limits.pesquisaRazaoSocial()) {
            beneficios.add("Busca por razão social");
        }
        if (limits.exportExcel()) {
            beneficios.add("Exportação Excel (.xlsx)");
        }
        if (limits.filtroSomenteAtivos()) {
            beneficios.add("Filtro de empresas ativas");
        }
        if (plan == SubscriptionPlan.PREMIUM) {
            beneficios.add(0, "7 dias grátis ao criar conta");
            beneficios.add("Histórico de 90 dias");
        }
        return beneficios;
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
        item.setMonthlyPriceCents(priceCents);
        if (priceCents == 0) {
            item.setPriceLabel("Grátis");
            item.setAnnualPriceCents(0);
            item.setAnnualPriceLabel("");
            item.setPaymentOptionsLabel("");
        } else {
            int annualCents = priceCents * 12;
            item.setPriceLabel(String.format("R$ %.2f/mês", priceCents / 100.0));
            item.setAnnualPriceCents(annualCents);
            item.setAnnualPriceLabel(String.format("R$ %.2f/ano", annualCents / 100.0));
            if (plan == SubscriptionPlan.PREMIUM) {
                item.setPaymentOptionsLabel("Inclui 7 dias grátis");
            } else {
                item.setPaymentOptionsLabel("À vista ou em até 12x no cartão");
            }
        }
        item.setBeneficios(beneficiosDe(plan, limits));
        item.setContatoComercial(false);
        return item;
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
