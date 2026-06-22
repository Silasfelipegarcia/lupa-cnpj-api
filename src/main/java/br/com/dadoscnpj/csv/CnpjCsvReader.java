package br.com.dadoscnpj.csv;

import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.util.CnpjValidator;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CnpjCsvReader {

    private static final Logger log = LoggerFactory.getLogger(CnpjCsvReader.class);

    private final CnpjPlanilhaParser parser;

    public CnpjCsvReader(CnpjPlanilhaParser parser) {
        this.parser = parser;
    }

    public List<ImportRow> lerLinhas(MultipartFile file) throws IOException {
        String conteudo = new String(file.getBytes(), StandardCharsets.UTF_8);
        conteudo = removerBom(conteudo);

        if (conteudo.isBlank()) {
            throw new IllegalArgumentException("Arquivo CSV vazio");
        }

        char separador = detectarSeparador(conteudo);

        try (CSVReader reader = new CSVReaderBuilder(new StringReader(conteudo))
                .withCSVParser(new CSVParserBuilder().withSeparator(separador).build())
                .build()) {

            List<String[]> linhas = reader.readAll();
            if (linhas.isEmpty()) {
                throw new IllegalArgumentException("Arquivo CSV vazio");
            }

            String[] cabecalho = linhas.get(0);
            for (int i = 0; i < cabecalho.length; i++) {
                cabecalho[i] = removerBom(cabecalho[i].trim());
            }

            int indiceCnpj = parser.encontrarIndice(cabecalho, "cnpj", "documento", "cnpj_cpf");
            int indiceRazaoSocial = parser.encontrarIndice(cabecalho, "razao_social", "razaosocial", "razao social", "empresa", "nome");

            if (indiceCnpj < 0 && indiceRazaoSocial < 0) {
                if (cabecalho.length == 1 && pareceCnpj(cabecalho[0])) {
                    return lerColunaUnica(linhas, true);
                }
                if (cabecalho.length == 1) {
                    indiceCnpj = 0;
                    log.info("CSV com coluna única '{}', tratando como CNPJ", cabecalho[0]);
                } else {
                    throw new IllegalArgumentException(
                            "O CSV deve conter a coluna 'cnpj' e/ou 'razao_social'. Colunas encontradas: "
                                    + String.join(", ", cabecalho));
                }
            }

            List<ImportRow> resultado = new ArrayList<>();
            for (int i = 1; i < linhas.size(); i++) {
                String[] linha = linhas.get(i);
                String cnpj = parser.lerCampo(linha, indiceCnpj);
                String razaoSocial = parser.lerCampoTexto(linha, indiceRazaoSocial);

                ImportRow importRow = parser.montarLinha(cnpj, razaoSocial);
                if (importRow != null) {
                    resultado.add(importRow);
                }
            }

            if (resultado.isEmpty()) {
                throw new IllegalArgumentException("Nenhuma linha com CNPJ ou razão social encontrada no arquivo");
            }

            log.info("Lidas {} linha(s) do arquivo {} (separador: '{}')",
                    resultado.size(), file.getOriginalFilename(), separador);
            return resultado;
        } catch (CsvException e) {
            throw new IOException("Erro ao ler CSV: " + e.getMessage(), e);
        }
    }

    private List<ImportRow> lerColunaUnica(List<String[]> linhas, boolean primeiraLinhaEhDado) {
        List<ImportRow> resultado = new ArrayList<>();
        int inicio = primeiraLinhaEhDado ? 0 : 1;

        if (primeiraLinhaEhDado) {
            ImportRow row = parser.montarLinha(linhas.get(0)[0], "");
            if (row != null) {
                resultado.add(row);
            }
            inicio = 1;
        }

        for (int i = inicio; i < linhas.size(); i++) {
            ImportRow row = parser.montarLinha(linhas.get(i)[0], "");
            if (row != null) {
                resultado.add(row);
            }
        }
        return resultado;
    }

    private char detectarSeparador(String conteudo) {
        String primeiraLinha = conteudo.lines().findFirst().orElse("");
        int pontoVirgula = contar(primeiraLinha, ';');
        int virgula = contar(primeiraLinha, ',');
        if (pontoVirgula > virgula) {
            return ';';
        }
        return ',';
    }

    private int contar(String texto, char c) {
        int count = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private String removerBom(String texto) {
        if (texto == null) {
            return "";
        }
        if (texto.startsWith("\ufeff")) {
            return texto.substring(1);
        }
        return texto;
    }

    private boolean pareceCnpj(String valor) {
        return CnpjValidator.removerMascara(valor).matches("\\d{14}");
    }
}
