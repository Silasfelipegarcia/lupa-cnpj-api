package br.com.lupainsights.service;

import br.com.lupainsights.client.CnpjPesquisaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CnpjResolucaoService implements CnpjResolucaoPort {

    private static final Logger log = LoggerFactory.getLogger(CnpjResolucaoService.class);

    private final CnpjPesquisaClient pesquisaClient;

    public CnpjResolucaoService(CnpjPesquisaClient pesquisaClient) {
        this.pesquisaClient = pesquisaClient;
    }

    public ResolucaoCnpj resolverPorRazaoSocial(String razaoSocial) throws InterruptedException {
        List<String> cnpjs = pesquisaClient.buscarPorRazaoSocial(razaoSocial);
        if (cnpjs.isEmpty()) {
            throw new CnpjPesquisaClient.CnpjPesquisaException(
                    "Nenhum CNPJ encontrado para a razão social: " + razaoSocial);
        }

        String cnpjEscolhido = cnpjs.getFirst();
        String aviso = null;
        if (cnpjs.size() > 1) {
            aviso = cnpjs.size() + " empresas encontradas; utilizado o CNPJ " + cnpjEscolhido;
            log.warn("Múltiplos CNPJs para '{}': {}", razaoSocial, cnpjs);
        }

        return new ResolucaoCnpj(cnpjEscolhido, aviso);
    }

    public record ResolucaoCnpj(String cnpj, String aviso) {
    }
}
