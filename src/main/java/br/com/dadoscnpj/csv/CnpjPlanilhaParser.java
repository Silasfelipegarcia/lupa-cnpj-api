package br.com.dadoscnpj.csv;

import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.util.CnpjEntradaNormalizer;
import br.com.dadoscnpj.util.CnpjValidator;
import org.springframework.stereotype.Component;

@Component
public class CnpjPlanilhaParser {

    public int encontrarIndice(String[] cabecalho, String... nomesPossiveis) {
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

    public String lerCampo(String[] linha, int indice) {
        if (indice < 0 || indice >= linha.length) {
            return "";
        }
        return CnpjEntradaNormalizer.normalizar(linha[indice]);
    }

    public String lerCampoTexto(String[] linha, int indice) {
        if (indice < 0 || indice >= linha.length) {
            return "";
        }
        return linha[indice] != null ? linha[indice].trim() : "";
    }

    public ImportRow montarLinha(String cnpj, String razaoSocial) {
        cnpj = CnpjEntradaNormalizer.normalizar(cnpj);
        razaoSocial = razaoSocial != null ? razaoSocial.trim() : "";

        if (cnpj.isEmpty() && razaoSocial.isEmpty()) {
            return null;
        }

        if (cnpj.isEmpty() && pareceCnpj(razaoSocial)) {
            cnpj = CnpjEntradaNormalizer.normalizar(razaoSocial);
            razaoSocial = "";
        }

        return new ImportRow(cnpj, razaoSocial);
    }

    public boolean isLinhaInstrucao(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String lower = texto.toLowerCase();
        return lower.startsWith("preencha pelo menos") || lower.startsWith("instru");
    }

    private String extrairCnpj(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return CnpjValidator.removerMascara(valor.trim());
    }

    private boolean pareceCnpj(String valor) {
        String digits = CnpjValidator.removerMascara(valor);
        return digits.matches("\\d{14}");
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

    private String removerBom(String texto) {
        if (texto != null && texto.startsWith("\ufeff")) {
            return texto.substring(1);
        }
        return texto;
    }
}
