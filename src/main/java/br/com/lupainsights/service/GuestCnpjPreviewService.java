package br.com.lupainsights.service;

import br.com.lupainsights.client.CnpjClient;
import br.com.lupainsights.config.SecurityProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.CnpjPreviewQuotaResponse;
import br.com.lupainsights.dto.CnpjPreviewResponse;
import br.com.lupainsights.dto.CnpjResponse;
import br.com.lupainsights.dto.CnpjResult;
import br.com.lupainsights.dto.ImportRow;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.plan.PlanLimits;
import br.com.lupainsights.plan.PlanLimitsService;
import br.com.lupainsights.util.CnpjValidator;
import br.com.lupainsights.util.IpRateLimiter;
import org.springframework.stereotype.Service;

@Service
public class GuestCnpjPreviewService {

    private final CnpjClient cnpjClient;
    private final IpRateLimiter ipRateLimiter;
    private final SecurityProperties securityProperties;
    private final PlanLimitsService planLimitsService;

    public GuestCnpjPreviewService(CnpjClient cnpjClient,
                                   IpRateLimiter ipRateLimiter,
                                   SecurityProperties securityProperties,
                                   PlanLimitsService planLimitsService) {
        this.cnpjClient = cnpjClient;
        this.ipRateLimiter = ipRateLimiter;
        this.securityProperties = securityProperties;
        this.planLimitsService = planLimitsService;
    }

    public CnpjPreviewQuotaResponse obterQuota(String clientIp) {
        int usadas = obterUso(clientIp);
        return new CnpjPreviewQuotaResponse(usadas, securityProperties.getGuestPreviewMax());
    }

    public CnpjPreviewResponse consultar(String clientIp, String cnpjInformado) throws InterruptedException {
        CnpjPreviewQuotaResponse quota = obterQuota(clientIp);
        if (quota.isLimiteAtingido()) {
            PlanLimits free = limitesPlanoFree();
            throw new IllegalStateException(
                    "Você já usou sua consulta gratuita completa. Crie uma conta grátis e consulte até "
                            + free.maxDirectCnpjPerDay() + " CNPJs únicos por dia, com até "
                            + free.maxBatchSearchesPerDay() + " empresas em planilha por dia.");
        }

        String cnpj = CnpjValidator.removerMascara(cnpjInformado);
        String erroCnpj = CnpjValidator.validar(cnpj);
        if (erroCnpj != null) {
            throw new IllegalArgumentException(erroCnpj);
        }

        CnpjResponse response;
        try {
            response = cnpjClient.consultar(cnpj);
        } catch (CnpjClient.CnpjConsultaException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        if (!registrarUso(clientIp)) {
            throw new IllegalStateException(
                    "Você já usou sua consulta gratuita completa. Crie uma conta para continuar consultando.");
        }

        CnpjPreviewQuotaResponse quotaAtualizada = obterQuota(clientIp);
        CnpjResult dados = CnpjResult.sucesso(new ImportRow(cnpj, null), cnpj, response, "");
        PlanLimits free = limitesPlanoFree();

        CnpjPreviewResponse preview = new CnpjPreviewResponse();
        preview.setCnpj(dados.getCnpj());
        preview.setRazaoSocial(valorOuPadrao(dados.getRazaoSocial(), "Razão social não informada na base"));
        preview.setNomeFantasia(dados.getNomeFantasia());
        preview.setSituacaoCadastral(dados.getSituacaoCadastral());
        preview.setTelefone1(dados.getTelefone1());
        preview.setTelefone2(dados.getTelefone2());
        preview.setEmail(dados.getEmail());
        preview.setLogradouro(dados.getLogradouro());
        preview.setNumero(dados.getNumero());
        preview.setComplemento(dados.getComplemento());
        preview.setBairro(dados.getBairro());
        preview.setCidade(dados.getCidade());
        preview.setUf(dados.getUf());
        preview.setCep(dados.getCep());
        preview.setCnaePrincipal(dados.getCnaePrincipal());
        preview.setConsultasUsadas(quotaAtualizada.getConsultasUsadas());
        preview.setConsultasLimite(quotaAtualizada.getConsultasLimite());
        preview.setConsultasRestantes(quotaAtualizada.getConsultasRestantes());
        preview.setCadastroLimiteCnpjDia(valorInteiro(free.maxDirectCnpjPerDay()));
        preview.setCadastroLimitePlanilha(free.maxRowsPerFile());
        preview.setCadastroLimiteLoteDia(valorInteiro(free.maxBatchSearchesPerDay()));
        return preview;
    }

    private int valorInteiro(Integer valor) {
        return valor != null ? valor : 0;
    }

    private PlanLimits limitesPlanoFree() {
        UserEntity free = new UserEntity();
        free.setRole(UserRole.USER);
        free.setPlan(SubscriptionPlan.FREE);
        return planLimitsService.limitesDe(free);
    }

    private String valorOuPadrao(String valor, String padrao) {
        return valor != null && !valor.isBlank() ? valor.trim() : padrao;
    }

    private int obterUso(String clientIp) {
        return ipRateLimiter.obterUso(chave(clientIp), janelaMs());
    }

    private boolean registrarUso(String clientIp) {
        return ipRateLimiter.registrarUsoSeAbaixoDoLimite(
                chave(clientIp),
                securityProperties.getGuestPreviewMax(),
                janelaMs()
        );
    }

    private String chave(String clientIp) {
        return "guest-preview:" + clientIp;
    }

    private long janelaMs() {
        return securityProperties.getGuestPreviewWindowDays() * 24L * 60L * 60L * 1000L;
    }
}
