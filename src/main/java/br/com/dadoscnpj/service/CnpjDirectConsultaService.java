package br.com.dadoscnpj.service;

import br.com.dadoscnpj.client.CnpjClient;
import br.com.dadoscnpj.dto.CnpjResponse;
import br.com.dadoscnpj.dto.CnpjResult;
import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.plan.UsageTrackingService;
import br.com.dadoscnpj.util.CnpjValidator;
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
