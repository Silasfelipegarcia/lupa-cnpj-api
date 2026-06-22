package br.com.dadoscnpj.service;

import br.com.dadoscnpj.client.CnpjClient;
import br.com.dadoscnpj.client.CnpjPesquisaClient;
import br.com.dadoscnpj.csv.CnpjPlanilhaReader;
import br.com.dadoscnpj.csv.CnpjCsvWriter;
import br.com.dadoscnpj.dto.CnpjResponse;
import br.com.dadoscnpj.dto.CnpjResult;
import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.util.CnpjValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class CnpjImportService {

    private static final Logger log = LoggerFactory.getLogger(CnpjImportService.class);

    private final CnpjPlanilhaReader planilhaReader;
    private final CnpjCsvWriter csvWriter;
    private final CnpjConsultaPort cnpjClient;
    private final CnpjResolucaoPort resolucaoService;

    public CnpjImportService(CnpjPlanilhaReader planilhaReader,
                             CnpjCsvWriter csvWriter,
                             CnpjConsultaPort cnpjClient,
                             CnpjResolucaoPort resolucaoService) {
        this.planilhaReader = planilhaReader;
        this.csvWriter = csvWriter;
        this.cnpjClient = cnpjClient;
        this.resolucaoService = resolucaoService;
    }

    public List<ImportRow> lerLinhasDoArquivo(MultipartFile file) throws IOException {
        return planilhaReader.ler(file);
    }

    public List<CnpjResult> processarLinhas(List<ImportRow> linhas, Consumer<ProgressoCnpj> onProgress)
            throws InterruptedException, IOException {
        List<CnpjResult> resultados = new ArrayList<>();
        int total = linhas.size();
        int atual = 0;

        for (ImportRow linha : linhas) {
            atual++;
            log.info("Processando linha {}/{}", atual, total);

            CnpjResult resultado = processarLinha(linha, atual);
            resultados.add(resultado);
            onProgress.accept(new ProgressoCnpj(
                    "SUCESSO".equals(resultado.getStatusConsulta()) ? "SUCESSO" : "ERRO",
                    resultado));
        }

        return resultados;
    }

    CnpjResult processarLinha(ImportRow linha, int numeroLinha) {
        try {
            normalizarLinha(linha);

            if (!linha.temCnpj() && !linha.temRazaoSocial()) {
                return CnpjResult.erro(linha, "Informe CNPJ ou razão social");
            }

            List<String> avisos = new ArrayList<>();
            boolean tentouCnpj = false;

            if (linha.temCnpj()) {
                tentouCnpj = true;
                String cnpjNorm = CnpjValidator.normalizarParaApi(linha.getCnpj());
                String erroValidacao = CnpjValidator.validar(cnpjNorm);

                if (erroValidacao == null) {
                    try {
                        log.info("Consultando CNPJ {} (linha {})", CnpjValidator.formatar(cnpjNorm), numeroLinha);
                        CnpjResponse response = cnpjClient.consultar(cnpjNorm);
                        adicionarAvisoRazaoSocialDivergente(avisos, linha, response);
                        return CnpjResult.sucesso(linha, cnpjNorm, response, montarObservacao(avisos));
                    } catch (CnpjClient.CnpjConsultaException e) {
                        log.warn("Falha na consulta por CNPJ (linha {}): {}", numeroLinha, e.getMessage());
                        avisos.add("CNPJ não encontrado na consulta direta");
                    }
                } else {
                    log.warn("CNPJ inválido na linha {}: {}", numeroLinha, erroValidacao);
                    avisos.add("CNPJ informado inválido");
                }
            }

            if (linha.temRazaoSocial()) {
                try {
                    CnpjResolucaoService.ResolucaoCnpj resolucao =
                            resolucaoService.resolverPorRazaoSocial(linha.getRazaoSocial());
                    String cnpjResolvido = CnpjValidator.normalizarParaApi(resolucao.cnpj());
                    adicionarAviso(resolucao.aviso(), avisos);

                    if (tentouCnpj) {
                        avisos.add("Dados obtidos por busca na razão social");
                    }

                    log.info("Consultando CNPJ {} via razão social (linha {})",
                            CnpjValidator.formatar(cnpjResolvido), numeroLinha);
                    CnpjResponse response = cnpjClient.consultar(cnpjResolvido);
                    return CnpjResult.sucesso(linha, cnpjResolvido, response, montarObservacao(avisos));
                } catch (CnpjPesquisaClient.CnpjPesquisaException e) {
                    log.error("Falha na pesquisa por razão social (linha {}): {}", numeroLinha, e.getMessage());
                    avisos.add(e.getMessage());
                } catch (CnpjClient.CnpjConsultaException e) {
                    log.error("Falha ao consultar CNPJ resolvido (linha {}): {}", numeroLinha, e.getMessage());
                    avisos.add(e.getMessage());
                }
            }

            return CnpjResult.erro(linha, montarMensagemErro(avisos));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CnpjResult.erro(linha, "Processamento interrompido");
        } catch (Exception e) {
            log.error("Erro inesperado na linha {}: {}", numeroLinha, e.getMessage());
            return CnpjResult.erro(linha, "Erro inesperado: " + e.getMessage());
        }
    }

    public byte[] gerarCsv(List<CnpjResult> resultados) throws IOException {
        return csvWriter.escrever(resultados);
    }

    private void normalizarLinha(ImportRow linha) {
        if (!linha.temCnpj() && linha.temRazaoSocial()) {
            String digits = CnpjValidator.removerMascara(linha.getRazaoSocial());
            if (digits.matches("\\d{14}")) {
                linha.setCnpj(digits);
                linha.setRazaoSocial("");
            }
        }
    }

    private void adicionarAvisoRazaoSocialDivergente(List<String> avisos, ImportRow linha, CnpjResponse response) {
        if (!linha.temRazaoSocial() || response == null) {
            return;
        }

        String informada = normalizarTexto(linha.getRazaoSocial());
        String encontrada = normalizarTexto(response.getRazaoSocial());
        if (encontrada.isEmpty() || informada.isEmpty()) {
            return;
        }

        if (!encontrada.contains(informada) && !informada.contains(encontrada)) {
            avisos.add("Razão social informada difere dos dados oficiais");
        }
    }

    private void adicionarAviso(String aviso, List<String> avisos) {
        if (aviso != null && !aviso.isBlank()) {
            avisos.add(aviso);
        }
    }

    private String montarObservacao(List<String> avisos) {
        if (avisos.isEmpty()) {
            return "";
        }
        return String.join("; ", avisos);
    }

    private String montarMensagemErro(List<String> avisos) {
        if (avisos.isEmpty()) {
            return "Não foi possível localizar a empresa";
        }
        return String.join("; ", avisos);
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toUpperCase().replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
