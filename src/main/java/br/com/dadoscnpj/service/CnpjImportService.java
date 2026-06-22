package br.com.dadoscnpj.service;

import br.com.dadoscnpj.config.CnpjApiProperties;
import br.com.dadoscnpj.client.CnpjClient;
import br.com.dadoscnpj.client.CnpjPesquisaClient;
import br.com.dadoscnpj.csv.CnpjPlanilhaReader;
import br.com.dadoscnpj.csv.CnpjCsvWriter;
import br.com.dadoscnpj.dto.CnpjResponse;
import br.com.dadoscnpj.dto.CnpjResult;
import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.util.CnpjEntradaNormalizer;
import br.com.dadoscnpj.util.CnpjValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Service
public class CnpjImportService {

    private static final Logger log = LoggerFactory.getLogger(CnpjImportService.class);

    private final CnpjPlanilhaReader planilhaReader;
    private final CnpjCsvWriter csvWriter;
    private final CnpjConsultaPort cnpjClient;
    private final CnpjResolucaoPort resolucaoService;
    private final CnpjApiProperties cnpjApiProperties;

    public CnpjImportService(CnpjPlanilhaReader planilhaReader,
                             CnpjCsvWriter csvWriter,
                             CnpjConsultaPort cnpjClient,
                             CnpjResolucaoPort resolucaoService,
                             CnpjApiProperties cnpjApiProperties) {
        this.planilhaReader = planilhaReader;
        this.csvWriter = csvWriter;
        this.cnpjClient = cnpjClient;
        this.resolucaoService = resolucaoService;
        this.cnpjApiProperties = cnpjApiProperties;
    }

    public List<ImportRow> lerLinhasDoArquivo(MultipartFile file) throws IOException {
        return planilhaReader.ler(file);
    }

    public List<CnpjResult> processarLinhas(List<ImportRow> linhas, Consumer<ProgressoCnpj> onProgress)
            throws InterruptedException, IOException {
        return processarLinhas(linhas, onProgress, null);
    }

    public List<CnpjResult> processarLinhas(List<ImportRow> linhas,
                                              Consumer<ProgressoCnpj> onProgress,
                                              BooleanSupplier continuarProcessamento)
            throws InterruptedException, IOException {
        List<CnpjResult> resultados = new ArrayList<>();
        Map<String, CnpjResult> cachePorCnpj = new HashMap<>();
        Map<String, CnpjResult> cachePorRazaoSocial = new HashMap<>();
        int total = linhas.size();
        int atual = 0;

        for (ImportRow linha : linhas) {
            if (continuarProcessamento != null && !continuarProcessamento.getAsBoolean()) {
                log.info("Processamento interrompido pelo usuário após {} de {} linha(s)", atual, total);
                break;
            }

            atual++;
            log.info("Processando linha {}/{}", atual, total);

            CnpjResult resultado = processarLinha(linha, atual, cachePorCnpj, cachePorRazaoSocial);
            resultados.add(resultado);
            onProgress.accept(new ProgressoCnpj(
                    "SUCESSO".equals(resultado.getStatusConsulta()) ? "SUCESSO" : "ERRO",
                    resultado));
        }

        return resultados;
    }

    CnpjResult processarLinha(ImportRow linha, int numeroLinha) {
        return processarLinha(linha, numeroLinha, new HashMap<>(), new HashMap<>());
    }

    CnpjResult processarLinha(ImportRow linha,
                              int numeroLinha,
                              Map<String, CnpjResult> cachePorCnpj,
                              Map<String, CnpjResult> cachePorRazaoSocial) {
        try {
            normalizarLinha(linha);

            if (!linha.temCnpj()) {
                if (!cnpjApiProperties.isPesquisaRazaoSocialAtiva()) {
                    CnpjResult erro = CnpjResult.erro(linha,
                            "Linha ignorada: informe o CNPJ (busca por razão social desligada).");
                    return registrarCache(linha, erro, null, cachePorCnpj, cachePorRazaoSocial);
                }
                if (!linha.temRazaoSocial()) {
                    return CnpjResult.erro(linha, "Informe CNPJ ou razão social");
                }
            }

            CnpjResult duplicado = buscarDuplicado(linha, cachePorCnpj, cachePorRazaoSocial);
            if (duplicado != null) {
                log.info("Linha {} duplicada; reutilizando resultado anterior", numeroLinha);
                return duplicado;
            }

            List<String> avisos = new ArrayList<>();
            boolean tentouCnpj = false;

            if (linha.temCnpj()) {
                tentouCnpj = true;
                String cnpjNorm = CnpjEntradaNormalizer.normalizar(linha.getCnpj());
                linha.setCnpj(cnpjNorm);
                String erroValidacao = CnpjValidator.validar(cnpjNorm);

                if (cnpjNorm.matches("\\d{14}")) {
                    CnpjResult emCache = cachePorCnpj.get(cnpjNorm);
                    if (isSucesso(emCache)) {
                        return CnpjResult.reutilizar(linha, emCache);
                    }

                    try {
                        log.info("Consultando CNPJ {} (linha {})", CnpjValidator.formatar(cnpjNorm), numeroLinha);
                        CnpjResponse response = cnpjClient.consultar(cnpjNorm);
                        if (erroValidacao != null) {
                            avisos.add("CNPJ com dígitos verificadores incorretos, mas encontrado na base");
                        }
                        adicionarAvisoRazaoSocialDivergente(avisos, linha, response);
                        return registrarCache(linha, CnpjResult.sucesso(linha, cnpjNorm, response, montarObservacao(avisos)),
                                cnpjNorm, cachePorCnpj, cachePorRazaoSocial);
                    } catch (CnpjClient.CnpjConsultaException e) {
                        log.warn("Falha na consulta por CNPJ (linha {}): {}", numeroLinha, e.getMessage());
                        if (erroValidacao != null) {
                            avisos.add("CNPJ informado inválido");
                        } else {
                            avisos.add("CNPJ não encontrado na consulta direta");
                        }
                    }
                } else if (erroValidacao != null) {
                    log.warn("CNPJ inválido na linha {}: {}", numeroLinha, erroValidacao);
                    avisos.add("CNPJ informado inválido");
                }
            }

            if (cnpjApiProperties.isPesquisaRazaoSocialAtiva() && linha.temRazaoSocial()) {
                String chaveRazao = chaveRazaoSocial(linha.getRazaoSocial());
                CnpjResult razaoEmCache = cachePorRazaoSocial.get(chaveRazao);
                if (isSucesso(razaoEmCache)) {
                    return CnpjResult.reutilizar(linha, razaoEmCache);
                }

                try {
                    CnpjResolucaoService.ResolucaoCnpj resolucao =
                            resolucaoService.resolverPorRazaoSocial(linha.getRazaoSocial());
                    String cnpjResolvido = CnpjValidator.normalizarParaApi(resolucao.cnpj());
                    adicionarAviso(resolucao.aviso(), avisos);

                    if (tentouCnpj) {
                        avisos.add("Dados obtidos por busca na razão social");
                    }

                    CnpjResult emCache = cachePorCnpj.get(cnpjResolvido);
                    if (isSucesso(emCache)) {
                        return registrarCache(linha, CnpjResult.reutilizar(linha, emCache),
                                cnpjResolvido, cachePorCnpj, cachePorRazaoSocial);
                    }

                    log.info("Consultando CNPJ {} via razão social (linha {})",
                            CnpjValidator.formatar(cnpjResolvido), numeroLinha);
                    CnpjResponse response = cnpjClient.consultar(cnpjResolvido);
                    return registrarCache(linha, CnpjResult.sucesso(linha, cnpjResolvido, response, montarObservacao(avisos)),
                            cnpjResolvido, cachePorCnpj, cachePorRazaoSocial);
                } catch (CnpjPesquisaClient.CnpjPesquisaException e) {
                    log.error("Falha na pesquisa por razão social (linha {}): {}", numeroLinha, e.getMessage());
                    avisos.add(e.getMessage());
                } catch (CnpjClient.CnpjConsultaException e) {
                    log.error("Falha ao consultar CNPJ resolvido (linha {}): {}", numeroLinha, e.getMessage());
                    avisos.add(e.getMessage());
                }
            }

            CnpjResult erro = CnpjResult.erro(linha, montarMensagemErro(avisos));
            registrarCache(linha, erro, chaveCnpj(linha), cachePorCnpj, cachePorRazaoSocial);
            return erro;
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

    private CnpjResult buscarDuplicado(ImportRow linha,
                                       Map<String, CnpjResult> cachePorCnpj,
                                       Map<String, CnpjResult> cachePorRazaoSocial) {
        String cnpj = chaveCnpj(linha);
        if (cnpj != null) {
            CnpjResult anterior = cachePorCnpj.get(cnpj);
            if (isSucesso(anterior)) {
                return CnpjResult.reutilizar(linha, anterior);
            }
        }

        if (linha.temRazaoSocial()) {
            CnpjResult anterior = cachePorRazaoSocial.get(chaveRazaoSocial(linha.getRazaoSocial()));
            if (isSucesso(anterior)) {
                return CnpjResult.reutilizar(linha, anterior);
            }
        }

        return null;
    }

    private boolean isSucesso(CnpjResult resultado) {
        return resultado != null && "SUCESSO".equals(resultado.getStatusConsulta());
    }

    private CnpjResult registrarCache(ImportRow linha,
                                        CnpjResult resultado,
                                        String cnpjChave,
                                        Map<String, CnpjResult> cachePorCnpj,
                                        Map<String, CnpjResult> cachePorRazaoSocial) {
        if (!isSucesso(resultado)) {
            return resultado;
        }

        if (cnpjChave != null && cnpjChave.matches("\\d{14}")) {
            cachePorCnpj.put(cnpjChave, resultado);
        } else {
            String cnpjResultado = extrairCnpjDoResultado(resultado);
            if (cnpjResultado != null) {
                cachePorCnpj.put(cnpjResultado, resultado);
            }
        }

        if (linha.temRazaoSocial()) {
            cachePorRazaoSocial.put(chaveRazaoSocial(linha.getRazaoSocial()), resultado);
        }

        return resultado;
    }

    private String chaveCnpj(ImportRow linha) {
        if (!linha.temCnpj()) {
            return null;
        }
        String cnpj = CnpjEntradaNormalizer.normalizar(linha.getCnpj());
        return cnpj.matches("\\d{14}") ? cnpj : null;
    }

    private String extrairCnpjDoResultado(CnpjResult resultado) {
        if (resultado.getCnpj() == null || resultado.getCnpj().isBlank()) {
            return null;
        }
        String cnpj = CnpjValidator.removerMascara(resultado.getCnpj());
        return cnpj.matches("\\d{14}") ? cnpj : null;
    }

    private String chaveRazaoSocial(String razaoSocial) {
        return normalizarTexto(razaoSocial);
    }

    private void normalizarLinha(ImportRow linha) {
        if (linha.temCnpj()) {
            linha.setCnpj(CnpjEntradaNormalizer.normalizar(linha.getCnpj()));
        }

        if (!linha.temCnpj() && linha.temRazaoSocial()) {
            String normalizado = CnpjEntradaNormalizer.normalizar(linha.getRazaoSocial());
            if (normalizado.matches("\\d{14}")) {
                linha.setCnpj(normalizado);
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
