package br.com.lupainsights.util;

public final class CnpjFormatter {

    private CnpjFormatter() {
    }

    public static String formatarTelefone(String ddd, String telefone) {
        if (ddd == null || ddd.isBlank() || telefone == null || telefone.isBlank()) {
            return "";
        }

        String numero = telefone.replaceAll("\\D", "");
        if (numero.length() == 9) {
            return String.format("(%s) %s-%s", ddd, numero.substring(0, 5), numero.substring(5));
        }
        if (numero.length() == 8) {
            return String.format("(%s) %s-%s", ddd, numero.substring(0, 4), numero.substring(4));
        }
        return String.format("(%s) %s", ddd, numero);
    }

    public static String formatarCep(String cep) {
        if (cep == null || cep.isBlank()) {
            return "";
        }
        String digits = cep.replaceAll("\\D", "");
        if (digits.length() == 8) {
            return digits.substring(0, 5) + "-" + digits.substring(5);
        }
        return cep;
    }

    public static String formatarLogradouro(String tipo, String logradouro) {
        String tipoFmt = tipo != null ? tipo.trim() : "";
        String logFmt = logradouro != null ? logradouro.trim() : "";

        if (tipoFmt.isEmpty()) {
            return logFmt;
        }
        if (logFmt.isEmpty()) {
            return tipoFmt;
        }
        if (logFmt.toUpperCase().startsWith(tipoFmt.toUpperCase())) {
            return logFmt;
        }
        return tipoFmt + " " + logFmt;
    }

    public static String formatarCnae(String codigo, String descricao) {
        String cod = codigo != null ? codigo.trim() : "";
        String desc = descricao != null ? descricao.trim() : "";

        if (cod.isEmpty()) {
            return desc;
        }
        if (desc.isEmpty()) {
            return cod;
        }
        return cod + " - " + desc;
    }

    public static String formatarCapitalSocial(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(valor.trim());
            java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(
                    java.util.Locale.forLanguageTag("pt-BR"));
            return formatter.format(amount);
        } catch (NumberFormatException e) {
            return valor.trim();
        }
    }
}
