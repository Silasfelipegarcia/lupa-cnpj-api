package br.com.lupainsights.util;

import br.com.lupainsights.dto.ImportRow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ImportLineDedupe {

    private ImportLineDedupe() {
    }

    public static List<ImportRow> deduplicar(List<ImportRow> linhas) {
        List<ImportRow> resultado = new ArrayList<>();
        Set<String> cnpjsVistos = new LinkedHashSet<>();
        Set<String> razoesVistas = new LinkedHashSet<>();

        for (ImportRow linha : linhas) {
            String chave = chave(linha);
            if (chave == null) {
                resultado.add(linha);
                continue;
            }
            if (linha.temCnpj()) {
                if (cnpjsVistos.add(chave)) {
                    resultado.add(linha);
                }
            } else if (razoesVistas.add(chave)) {
                resultado.add(linha);
            }
        }
        return resultado;
    }

    private static String chave(ImportRow linha) {
        if (linha.temCnpj()) {
            String cnpj = CnpjValidator.removerMascara(linha.getCnpj());
            return cnpj.matches("\\d{14}") ? cnpj : null;
        }
        if (linha.temRazaoSocial()) {
            return linha.getRazaoSocial().trim().toUpperCase();
        }
        return null;
    }
}
