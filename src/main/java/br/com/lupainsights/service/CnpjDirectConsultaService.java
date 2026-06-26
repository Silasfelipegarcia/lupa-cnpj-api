package br.com.lupainsights.service;

import br.com.lupainsights.client.CnpjClient;
import br.com.lupainsights.dto.CnpjResponse;
import br.com.lupainsights.dto.CnpjResult;
import br.com.lupainsights.dto.ImportRow;
import br.com.lupainsights.plan.CnpjResultMaskingService;
import br.com.lupainsights.plan.UsageTrackingService;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.util.CnpjValidator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CnpjDirectConsultaService {

    private final CnpjClient cnpjClient;
    private final UsageTrackingService usageTrackingService;
    private final UserRepository userRepository;
    private final CnpjResultMaskingService maskingService;

    public CnpjDirectConsultaService(CnpjClient cnpjClient,
                                     UsageTrackingService usageTrackingService,
                                     UserRepository userRepository,
                                     CnpjResultMaskingService maskingService) {
        this.cnpjClient = cnpjClient;
        this.usageTrackingService = usageTrackingService;
        this.userRepository = userRepository;
        this.maskingService = maskingService;
    }

    public CnpjResult consultar(UUID userId, String cnpjInformado) throws InterruptedException {
        String cnpj = CnpjValidator.removerMascara(cnpjInformado);
        String erroCnpj = CnpjValidator.validar(cnpj);
        if (erroCnpj != null) {
            throw new IllegalArgumentException(erroCnpj);
        }

        usageTrackingService.validarEIncrementarDirectCnpj(userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        try {
            CnpjResponse response = cnpjClient.consultar(cnpj);
            ImportRow linha = new ImportRow();
            linha.setCnpj(cnpj);
            CnpjResult result = CnpjResult.sucesso(linha, cnpj, response, "");
            return maskingService.aplicarSeNecessario(user, result);
        } catch (CnpjClient.CnpjConsultaException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
