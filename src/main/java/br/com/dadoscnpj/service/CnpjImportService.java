package br.com.dadoscnpj.service;

import br.com.dadoscnpj.client.CnpjClient;
import br.com.dadoscnpj.client.CnpjPesquisaClient;
import br.com.dadoscnpj.csv.CnpjCsvReader;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class CnpjImportService {

    private static final Logger log = LoggerFactory.getLogger(CnpjImportService.class);

    private final CnpjCsvReader csvReader;
    private final CnpjCsvWriter csvWriter;
    private final CnpjClient cnpjClient;
    private final CnpjResolucaoService resolucaoService;

    public CnpjImportService(CnpjCsvReader csvReader,
                             CnpjCsvWriter csvWriter,
                             CnpjClient cnpjClient,
                             CnpjResolucaoService resolucaoService) {
        this.csvReader = csvReader;
        this.csvWriter = csvWriter;
        this.cnpjClient = cnpjClient;
        this.resolucaoService = resolucaoService;
    }

    public List<ImportRow> lerLinhasDoArquivo(MultipartFile file) throws IOException {
        return csvReader.lerLinhas(file);
    }

    public List<CnpjResult> processarLinhas(List<ImportRow> linhas, Consumer<ProgressoCnpj> onProgress)
            throws InterruptedException, IOException {
        List<CnpjResult> resultados = new ArrayList<>();
        int total = linhas.size();
        int atual = 0;

        for (ImportRow linha : linhas) {
            atual++;
            log.info("Processando linha {}/{}", atual, total);

            if (!linha.temCnpj() && !linha.temRazaoSocial()) {
                CnpjResult resultado = CnpjResult.erro(linha, "Informe CNPJ ou razão social");
                resultados.add(resultado);
                onProgress.accept(new ProgressoCnpj("ERRO", resultado));
                continue;
            }

            try {
                normalizarLinha(linha);

                String cnpjResolvido;
                String aviso = null;

                if (linha.temCnpj()) {
                    cnpjResolvido = CnpjValidator.normalizarParaApi(linha.getCnpj());
                    String erroValidacao = CnpjValidator.validar(cnpjResolvido);
                    if (erroValidacao != null) {
                        CnpjResult resultado = CnpjResult.erro(linha, erroValidacao);
                        resultados.add(resultado);
                        onProgress.accept(new ProgressoCnpj("ERRO", resultado));
                        continue;
                    }
                } else {
                    CnpjResolucaoService.ResolucaoCnpj resolucao = resolucaoService.resolver(linha);
                    cnpjResolvido = CnpjValidator.normalizarParaApi(resolucao.cnpj());
                    aviso = resolucao.aviso();
                }

                log.info("Consultando CNPJ {} (linha {})", CnpjValidator.formatar(cnpjResolvido), atual);
                CnpjResponse response = cnpjClient.consultar(cnpjResolvido);
                CnpjResult resultado = CnpjResult.sucesso(linha, cnpjResolvido, response, aviso);
                resultados.add(resultado);
                onProgress.accept(new ProgressoCnpj("SUCESSO", resultado));
            } catch (CnpjPesquisaClient.CnpjPesquisaException e) {
                log.error("Falha na pesquisa: {}", e.getMessage());
                CnpjResult resultado = CnpjResult.erro(linha, e.getMessage());
                resultados.add(resultado);
                onProgress.accept(new ProgressoCnpj("ERRO", resultado));
            } catch (CnpjClient.CnpjConsultaException e) {
                log.error("Falha ao consultar CNPJ: {}", e.getMessage());
                CnpjResult resultado = CnpjResult.erro(linha, e.getMessage());
                resultados.add(resultado);
                onProgress.accept(new ProgressoCnpj("ERRO", resultado));
            } catch (Exception e) {
                log.error("Erro inesperado: {}", e.getMessage());
                CnpjResult resultado = CnpjResult.erro(linha, "Erro inesperado: " + e.getMessage());
                resultados.add(resultado);
                onProgress.accept(new ProgressoCnpj("ERRO", resultado));
            }
        }

        return resultados;
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
}
