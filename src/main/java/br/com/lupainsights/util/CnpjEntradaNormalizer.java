package br.com.lupainsights.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CnpjEntradaNormalizer {

    private static final DecimalFormat CNPJ_NUMERICO = criarFormatadorCnpj();

    private CnpjEntradaNormalizer() {
    }

    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }

        String texto = valor.trim();

        if (pareceNotacaoCientifica(texto)) {
            try {
                BigDecimal numero = new BigDecimal(texto.replace(",", "."));
                return CNPJ_NUMERICO.format(numero);
            } catch (NumberFormatException ignored) {
                // segue para normalização padrão
            }
        }

        String digits = CnpjValidator.removerMascara(texto);
        if (digits.isEmpty()) {
            return "";
        }

        if (digits.length() < 14) {
            String semZeros = new BigDecimal(digits).toBigInteger().toString();
            digits = "0".repeat(Math.max(0, 14 - semZeros.length())) + semZeros;
        } else if (digits.length() > 14) {
            digits = digits.substring(0, 14);
        }

        return digits;
    }

    private static boolean pareceNotacaoCientifica(String texto) {
        return texto.matches("(?i)^\\d+(?:[.,]\\d+)?[eE][+-]?\\d+$");
    }

    private static DecimalFormat criarFormatadorCnpj() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("00000000000000", symbols);
        format.setParseBigDecimal(true);
        return format;
    }
}
