package br.com.lupainsights.plan;

import br.com.lupainsights.dto.CnpjResult;
import br.com.lupainsights.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CnpjResultMaskingService {

    private static final String UPGRADE_HINT = "Disponível no plano Prospecção";

    private final PlanLimitsService planLimitsService;

    public CnpjResultMaskingService(PlanLimitsService planLimitsService) {
        this.planLimitsService = planLimitsService;
    }

    public CnpjResult aplicarSeNecessario(UserEntity user, CnpjResult result) {
        if (result == null || !"SUCESSO".equalsIgnoreCase(result.getStatusConsulta())) {
            return result;
        }
        if (!planLimitsService.limitesDe(user).dadosLimitados()) {
            return result;
        }
        result.setTelefone1(UPGRADE_HINT);
        result.setTelefone2(null);
        result.setEmail(UPGRADE_HINT);
        result.setLogradouro(UPGRADE_HINT);
        result.setNumero(null);
        result.setComplemento(null);
        result.setBairro(null);
        if (result.getCidade() != null && result.getUf() != null) {
            result.setCep("*****-***");
        } else {
            result.setCep(UPGRADE_HINT);
        }
        if (result.getCnaePrincipal() != null && result.getCnaePrincipal().length() > 12) {
            result.setCnaePrincipal(result.getCnaePrincipal().substring(0, 12) + "…");
        }
        return result;
    }

    public List<CnpjResult> aplicarSeNecessario(UserEntity user, List<CnpjResult> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return resultados;
        }
        if (!planLimitsService.limitesDe(user).dadosLimitados()) {
            return resultados;
        }
        return resultados.stream()
                .map(r -> aplicarSeNecessario(user, r))
                .toList();
    }
}
