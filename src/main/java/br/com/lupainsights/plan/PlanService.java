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
        itens.add(itemTrialGratis());
        itens.add(item(SubscriptionPlan.PREMIUM, mercadoPagoProperties.getPremiumPriceCents()));
        itens.add(item(SubscriptionPlan.PRO_PLUS, mercadoPagoProperties.getProPlusPriceCents()));
        if (incluirPlanoAdmin) {
            itens.add(itemAdminTest());
        }
        itens.add(itemBusiness());
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

    private PlanCatalogItemResponse itemTrialGratis() {
        PlanLimits limits = planLimitsService.limitesDe(planoTemporario(SubscriptionPlan.PREMIUM));
        PlanCatalogItemResponse item = new PlanCatalogItemResponse();
        item.setPlan(SubscriptionPlan.FREE);
        item.setNome("Trial 7 dias");
        item.setDescricao("Prospecção completa · ativado ao confirmar o e-mail");
        item.setMaxRowsPerFile(limits.maxRowsPerFile());
        item.setBatchSearchesPerDay(formatarLimitePlanilhas(limits));
        item.setDirectCnpjPerDay(formatarLimiteDirect(SubscriptionPlan.PREMIUM, limits));
        item.setPriceCents(0);
        item.setMonthlyPriceCents(0);
        item.setAnnualPriceCents(0);
        item.setPriceLabel("7 dias grátis");
        item.setAnnualPriceLabel("");
        item.setPaymentOptionsLabel("Sem cartão para começar");
        item.setBeneficios(beneficiosTrial(limits));
        item.setContatoComercial(false);
        return item;
    }

    private List<String> beneficiosTrial(PlanLimits limits) {
        List<String> beneficios = new ArrayList<>();
        beneficios.add("Até " + limits.maxRowsPerFile() + " empresas por planilha");
        beneficios.add(formatarLimitePlanilhas(limits));
        beneficios.add("CNPJ único: " + formatarLimiteDirect(SubscriptionPlan.PREMIUM, limits) + " por dia");
        beneficios.add("Busca por razão social");
        beneficios.add("Exportação Excel (.xlsx)");
        beneficios.add("Filtro de empresas ativas");
        beneficios.add("Histórico de 90 dias");
        beneficios.add("Cadastre cartão antes do fim para manter o acesso");
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
            item.setPaymentOptionsLabel("À vista ou em até 12x no cartão");
        }
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
        item.setMonthlyPriceCents(0);
        item.setAnnualPriceCents(0);
        item.setPriceLabel("Fale conosco");
        item.setAnnualPriceLabel("");
        item.setPaymentOptionsLabel("");
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
