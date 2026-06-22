package br.com.dadoscnpj.service;

import br.com.dadoscnpj.client.CnpjClient;
import br.com.dadoscnpj.config.SecurityProperties;
import br.com.dadoscnpj.dto.CnpjPreviewQuotaResponse;
import br.com.dadoscnpj.dto.CnpjPreviewResponse;
import br.com.dadoscnpj.dto.CnpjResponse;
import br.com.dadoscnpj.util.CnpjValidator;
import br.com.dadoscnpj.util.IpRateLimiter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuestCnpjPreviewService {

    private static final List<String> CAMPOS_COM_LOGIN = List.of(
            "Nome fantasia",
            "Situação cadastral",
            "Telefones",
            "E-mail",
            "Endereço completo",
            "Cidade e UF",
            "CNAE principal"
    );

    private final CnpjClient cnpjClient;
    private final IpRateLimiter ipRateLimiter;
    private final SecurityProperties securityProperties;

    public GuestCnpjPreviewService(CnpjClient cnpjClient,
                                   IpRateLimiter ipRateLimiter,
                                   SecurityProperties securityProperties) {
        this.cnpjClient = cnpjClient;
        this.ipRateLimiter = ipRateLimiter;
        this.securityProperties = securityProperties;
    }

    public CnpjPreviewQuotaResponse obterQuota(String clientIp) {
        int usadas = obterUso(clientIp);
        return new CnpjPreviewQuotaResponse(usadas, securityProperties.getGuestPreviewMax());
    }

    public CnpjPreviewResponse consultar(String clientIp, String cnpjInformado) throws InterruptedException {
        CnpjPreviewQuotaResponse quota = obterQuota(clientIp);
        if (quota.isLimiteAtingido()) {
            throw new IllegalStateException(
                    "Você atingiu o limite de " + quota.getConsultasLimite()
                            + " consultas gratuitas. Crie uma conta para consultar sem limite.");
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
                    "Você atingiu o limite de consultas gratuitas. Crie uma conta para continuar.");
        }

        CnpjPreviewQuotaResponse quotaAtualizada = obterQuota(clientIp);

        CnpjPreviewResponse preview = new CnpjPreviewResponse();
        preview.setCnpj(CnpjValidator.formatar(cnpj));
        preview.setRazaoSocial(extrairRazaoSocial(response));
        preview.setConsultasUsadas(quotaAtualizada.getConsultasUsadas());
        preview.setConsultasLimite(quotaAtualizada.getConsultasLimite());
        preview.setConsultasRestantes(quotaAtualizada.getConsultasRestantes());
        preview.setCamposComLogin(CAMPOS_COM_LOGIN);
        return preview;
    }

    private String extrairRazaoSocial(CnpjResponse response) {
        if (response.getRazaoSocial() != null && !response.getRazaoSocial().isBlank()) {
            return response.getRazaoSocial().trim();
        }
        if (response.getEstabelecimento() != null
                && response.getEstabelecimento().getNomeFantasia() != null
                && !response.getEstabelecimento().getNomeFantasia().isBlank()) {
            return response.getEstabelecimento().getNomeFantasia().trim();
        }
        return "Razão social não informada na base";
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
