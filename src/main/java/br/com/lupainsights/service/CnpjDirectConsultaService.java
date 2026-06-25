package br.com.lupainsights.service;

import br.com.lupainsights.client.CnpjClient;
import br.com.lupainsights.dto.CnpjResponse;
import br.com.lupainsights.dto.CnpjResult;
import br.com.lupainsights.dto.ImportRow;
import br.com.lupainsights.plan.UsageTrackingService;
import br.com.lupainsights.util.CnpjValidator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CnpjDirectConsultaService {

    private final CnpjClient cnpjClient;
    private final UsageTrackingService usageTrackingService;

    public CnpjDirectConsultaService(CnpjClient cnpjClient, UsageTrackingService usageTrackingService) {
        this.cnpjClient = cnpjClient;
        this.usageTrackingService = usageTrackingService;
    }

    public CnpjResult consultar(UUID userId, String cnpjInformado) throws InterruptedException {
        String cnpj = CnpjValidator.removerMascara(cnpjInformado);
        String erroCnpj = CnpjValidator.validar(cnpj);
        if (erroCnpj != null) {
            throw new IllegalArgumentException(erroCnpj);
        }

        usageTrackingService.validarEIncrementarDirectCnpj(userId);

        try {
            CnpjResponse response = cnpjClient.consultar(cnpj);
            ImportRow linha = new ImportRow();
            linha.setCnpj(cnpj);
            return CnpjResult.sucesso(linha, cnpj, response, "");
        } catch (CnpjClient.CnpjConsultaException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
