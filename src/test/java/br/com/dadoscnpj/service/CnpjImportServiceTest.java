package br.com.dadoscnpj.service;

import br.com.dadoscnpj.client.CnpjClient;
import br.com.dadoscnpj.client.CnpjPesquisaClient;
import br.com.dadoscnpj.config.CnpjApiProperties;
import br.com.dadoscnpj.dto.CnpjResponse;
import br.com.dadoscnpj.dto.ImportRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CnpjImportServiceTest {

    private FakeCnpjConsulta cnpjConsulta;
    private FakeRazaoSocialResolucao resolucao;
    private CnpjApiProperties cnpjApiProperties;

    private CnpjImportService criarService() {
        return new CnpjImportService(null, null, cnpjConsulta, resolucao, cnpjApiProperties);
    }

    @BeforeEach
    void setUp() {
        cnpjConsulta = new FakeCnpjConsulta();
        resolucao = new FakeRazaoSocialResolucao();
        cnpjApiProperties = pesquisaHabilitada();
    }

    private CnpjApiProperties pesquisaHabilitada() {
        CnpjApiProperties properties = new CnpjApiProperties();
        properties.setPesquisaRazaoSocialHabilitada(true);
        properties.setToken("token-teste");
        return properties;
    }

    private CnpjApiProperties pesquisaDesligada() {
        CnpjApiProperties properties = new CnpjApiProperties();
        properties.setPesquisaRazaoSocialHabilitada(false);
        return properties;
    }

    @Test
    void deveConsultarPorCnpjValido() throws Exception {
        CnpjImportService service = criarService();
        CnpjResponse response = new CnpjResponse();
        response.setRazaoSocial("EMPRESA TESTE");
        cnpjConsulta.enfileirarSucesso(response);

        var resultado = service.processarLinha(new ImportRow("19131243000197", ""), 1);

        assertEquals("SUCESSO", resultado.getStatusConsulta());
        assertEquals(1, cnpjConsulta.chamadas);
        assertEquals(0, resolucao.chamadas);
    }

    @Test
    void deveTentarRazaoSocialQuandoCnpjInvalido() throws Exception {
        CnpjImportService service = criarService();
        CnpjResponse response = new CnpjResponse();
        response.setRazaoSocial("PETROLEO BRASILEIRO S A PETROBRAS");
        cnpjConsulta.enfileirarErro(new CnpjClient.CnpjConsultaException("CNPJ não encontrado", null));
        resolucao.proximo = new CnpjResolucaoService.ResolucaoCnpj("33000167000101", null);
        cnpjConsulta.enfileirarSucesso(response);

        var resultado = service.processarLinha(new ImportRow("19131243000100", "PETROBRAS"), 1);

        assertEquals("SUCESSO", resultado.getStatusConsulta());
        assertTrue(resultado.getObservacao().contains("CNPJ informado inválido"));
        assertTrue(resultado.getObservacao().contains("Dados obtidos por busca na razão social"));
        assertEquals(2, cnpjConsulta.chamadas);
        assertEquals("33000167000101", cnpjConsulta.ultimoCnpj);
        assertEquals(1, resolucao.chamadas);
    }

    @Test
    void deveTentarRazaoSocialQuandoCnpjNaoEncontrado() throws Exception {
        CnpjImportService service = criarService();
        CnpjResponse response = new CnpjResponse();
        response.setRazaoSocial("EMPRESA TESTE LTDA");
        cnpjConsulta.enfileirarErro(new CnpjClient.CnpjConsultaException("CNPJ não encontrado na API pública", null));
        cnpjConsulta.enfileirarSucesso(response);
        resolucao.proximo = new CnpjResolucaoService.ResolucaoCnpj("19131243000197", null);

        var resultado = service.processarLinha(new ImportRow("19131243000197", "EMPRESA TESTE"), 1);

        assertEquals("SUCESSO", resultado.getStatusConsulta());
        assertTrue(resultado.getObservacao().contains("CNPJ não encontrado na consulta direta"));
        assertEquals(2, cnpjConsulta.chamadas);
        assertEquals(1, resolucao.chamadas);
    }

    @Test
    void deveRetornarErroQuandoAmbosFalharem() throws Exception {
        CnpjImportService service = criarService();
        cnpjConsulta.enfileirarErro(new CnpjClient.CnpjConsultaException("CNPJ não encontrado", null));
        resolucao.erro = new CnpjPesquisaClient.CnpjPesquisaException("Nenhum CNPJ encontrado");

        var resultado = service.processarLinha(new ImportRow("19131243000100", "INEXISTENTE"), 1);

        assertEquals("ERRO", resultado.getStatusConsulta());
        assertTrue(resultado.getErro().contains("CNPJ informado inválido"));
        assertTrue(resultado.getErro().contains("Nenhum CNPJ encontrado"));
        assertEquals(1, cnpjConsulta.chamadas);
    }

    @Test
    void naoDeveConsultarCnpjDuplicadoNaMesmaImportacao() throws Exception {
        CnpjImportService service = criarService();
        CnpjResponse response = new CnpjResponse();
        response.setRazaoSocial("EMPRESA TESTE");
        cnpjConsulta.enfileirarSucesso(response);

        Map<String, br.com.dadoscnpj.dto.CnpjResult> cacheCnpj = new HashMap<>();
        Map<String, br.com.dadoscnpj.dto.CnpjResult> cacheRazao = new HashMap<>();

        var primeiro = service.processarLinha(new ImportRow("19131243000197", ""), 1, cacheCnpj, cacheRazao);
        var segundo = service.processarLinha(new ImportRow("19.131.243/0001-97", ""), 2, cacheCnpj, cacheRazao);

        assertEquals("SUCESSO", primeiro.getStatusConsulta());
        assertEquals("SUCESSO", segundo.getStatusConsulta());
        assertTrue(segundo.getObservacao().contains("duplicado"));
        assertEquals(1, cnpjConsulta.chamadas);
    }

    @Test
    void naoDeveConsultarRazaoSocialDuplicadaNaMesmaImportacao() throws Exception {
        CnpjImportService service = criarService();
        CnpjResponse response = new CnpjResponse();
        response.setRazaoSocial("PETROBRAS");
        resolucao.proximo = new CnpjResolucaoService.ResolucaoCnpj("33000167000101", null);
        cnpjConsulta.enfileirarSucesso(response);

        Map<String, br.com.dadoscnpj.dto.CnpjResult> cacheCnpj = new HashMap<>();
        Map<String, br.com.dadoscnpj.dto.CnpjResult> cacheRazao = new HashMap<>();

        service.processarLinha(new ImportRow("", "PETROBRAS"), 1, cacheCnpj, cacheRazao);
        var segundo = service.processarLinha(new ImportRow("", "petrobras"), 2, cacheCnpj, cacheRazao);

        assertEquals("SUCESSO", segundo.getStatusConsulta());
        assertTrue(segundo.getObservacao().contains("duplicado"));
        assertEquals(1, cnpjConsulta.chamadas);
        assertEquals(1, resolucao.chamadas);
    }

    @Test
    void deveIgnorarLinhaSoComRazaoSocialQuandoPesquisaDesligada() {
        cnpjApiProperties = pesquisaDesligada();
        CnpjImportService service = criarService();

        var resultado = service.processarLinha(new ImportRow("", "PETROBRAS"), 1);

        assertEquals("ERRO", resultado.getStatusConsulta());
        assertTrue(resultado.getErro().contains("razão social desligada"));
        assertEquals(0, cnpjConsulta.chamadas);
        assertEquals(0, resolucao.chamadas);
    }

    private static class FakeCnpjConsulta implements CnpjConsultaPort {
        private final Deque<Object> respostas = new ArrayDeque<>();
        int chamadas;
        String ultimoCnpj;

        void enfileirarSucesso(CnpjResponse response) {
            respostas.add(response);
        }

        void enfileirarErro(CnpjClient.CnpjConsultaException erro) {
            respostas.add(erro);
        }

        @Override
        public CnpjResponse consultar(String cnpj) {
            chamadas++;
            ultimoCnpj = cnpj;
            if (respostas.isEmpty()) {
                throw new CnpjClient.CnpjConsultaException("Sem resposta configurada no teste", null);
            }
            Object proximo = respostas.removeFirst();
            if (proximo instanceof CnpjResponse response) {
                return response;
            }
            throw (RuntimeException) proximo;
        }
    }

    private static class FakeRazaoSocialResolucao implements CnpjResolucaoPort {
        int chamadas;
        CnpjResolucaoService.ResolucaoCnpj proximo;
        RuntimeException erro;

        @Override
        public CnpjResolucaoService.ResolucaoCnpj resolverPorRazaoSocial(String razaoSocial) {
            chamadas++;
            if (erro != null) {
                throw erro;
            }
            return proximo;
        }
    }
}
