package br.com.dadoscnpj.util;

public final class CnpjValidator {

    private static final int[] PESOS_DIGITO_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DIGITO_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    public static String removerMascara(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("\\D", "");
    }

    public static boolean isValido(String cnpj) {
        return validar(cnpj) == null;
    }

    public static String validar(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return "CNPJ vazio";
        }

        if (!cnpj.matches("\\d{14}")) {
            return "CNPJ inválido: deve conter exatamente 14 dígitos numéricos";
        }

        if (cnpj.chars().distinct().count() == 1) {
            return "CNPJ inválido: dígitos repetidos";
        }

        if (!digitosVerificadoresValidos(cnpj)) {
            return "CNPJ inválido: dígitos verificadores incorretos";
        }

        return null;
    }

    private static boolean digitosVerificadoresValidos(String cnpj) {
        int digito1 = calcularDigito(cnpj, PESOS_DIGITO_1, 12);
        if (digito1 != charParaDigito(cnpj.charAt(12))) {
            return false;
        }

        int digito2 = calcularDigito(cnpj, PESOS_DIGITO_2, 13);
        return digito2 == charParaDigito(cnpj.charAt(13));
    }

    private static int calcularDigito(String cnpj, int[] pesos, int tamanho) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += charParaDigito(cnpj.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int charParaDigito(char c) {
        return c - '0';
    }

    /** Formato exibido: XX.XXX.XXX/XXXX-XX */
    public static String formatar(String cnpj) {
        String digits = removerMascara(cnpj);
        if (digits.length() != 14) {
            return digits;
        }
        return String.format("%s.%s.%s/%s-%s",
                digits.substring(0, 2),
                digits.substring(2, 5),
                digits.substring(5, 8),
                digits.substring(8, 12),
                digits.substring(12, 14));
    }

    /** API consulta com 14 dígitos sem máscara */
    public static String normalizarParaApi(String cnpj) {
        return removerMascara(cnpj);
    }
}
