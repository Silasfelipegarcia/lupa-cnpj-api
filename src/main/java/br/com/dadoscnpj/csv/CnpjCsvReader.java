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

            int indiceCnpj = encontrarIndice(cabecalho, "cnpj", "documento", "cnpj_cpf");
            int indiceRazaoSocial = encontrarIndice(cabecalho, "razao_social", "razaosocial", "razao social", "empresa", "nome");

            if (indiceCnpj < 0 && indiceRazaoSocial < 0) {
                if (cabecalho.length == 1 && pareceCnpj(cabecalho[0])) {
                    return lerColunaUnicaSemCabecalhoValido(linhas, true);
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
                String cnpj = lerCampo(linha, indiceCnpj);
                String razaoSocial = lerCampo(linha, indiceRazaoSocial);

                cnpj = extrairCnpj(cnpj);
                razaoSocial = razaoSocial != null ? razaoSocial.trim() : "";

                if (cnpj.isEmpty() && razaoSocial.isEmpty()) {
                    continue;
                }

                if (cnpj.isEmpty() && pareceCnpj(razaoSocial)) {
                    cnpj = CnpjValidator.removerMascara(razaoSocial);
                    razaoSocial = "";
                    log.debug("Linha {}: valor na coluna razão social identificado como CNPJ", i + 1);
                }

                resultado.add(new ImportRow(cnpj, razaoSocial));
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

    private List<ImportRow> lerColunaUnicaSemCabecalhoValido(List<String[]> linhas, boolean primeiraLinhaEhDado) {
        List<ImportRow> resultado = new ArrayList<>();
        int inicio = primeiraLinhaEhDado ? 0 : 1;

        if (primeiraLinhaEhDado) {
            String cnpj = extrairCnpj(linhas.get(0)[0]);
            if (!cnpj.isEmpty()) {
                resultado.add(new ImportRow(cnpj, ""));
            }
            inicio = 1;
        }

        for (int i = inicio; i < linhas.size(); i++) {
            String cnpj = extrairCnpj(linhas.get(i)[0]);
            if (!cnpj.isEmpty()) {
                resultado.add(new ImportRow(cnpj, ""));
            }
        }
        return resultado;
    }

    private String lerCampo(String[] linha, int indice) {
        if (indice < 0 || indice >= linha.length) {
            return "";
        }
        return removerBom(linha[indice].trim());
    }

    private String extrairCnpj(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return CnpjValidator.removerMascara(valor);
    }

    private boolean pareceCnpj(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String digits = CnpjValidator.removerMascara(valor);
        return digits.matches("\\d{14}");
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

    private int encontrarIndice(String[] cabecalho, String... nomesPossiveis) {
        for (int i = 0; i < cabecalho.length; i++) {
            String coluna = normalizarNomeColuna(cabecalho[i]);
            for (String nome : nomesPossiveis) {
                if (coluna.equals(normalizarNomeColuna(nome))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String normalizarNomeColuna(String nome) {
        if (nome == null) {
            return "";
        }
        return removerBom(nome.trim())
                .toLowerCase()
                .replace("ã", "a")
                .replace("á", "a")
                .replace("à", "a")
                .replace("â", "a")
                .replace("ç", "c")
                .replace(" ", "_")
                .replace("-", "_");
    }
}
