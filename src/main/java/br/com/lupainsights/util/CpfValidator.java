package br.com.lupainsights.util;

public final class CpfValidator {

    private CpfValidator() {
    }

    public static String removerMascara(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.replaceAll("\\D", "");
    }

    public static String validar(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return "CPF vazio";
        }

        String digits = removerMascara(cpf);
        if (!digits.matches("\\d{11}")) {
            return "CPF inválido: deve conter 11 dígitos";
        }

        if (digits.chars().distinct().count() == 1) {
            return "CPF inválido: dígitos repetidos";
        }

        if (!digitosVerificadoresValidos(digits)) {
            return "CPF inválido: dígitos verificadores incorretos";
        }

        return null;
    }

    private static boolean digitosVerificadoresValidos(String cpf) {
        int digito1 = calcularDigito(cpf, 9, 10);
        if (digito1 != charParaDigito(cpf.charAt(9))) {
            return false;
        }
        int digito2 = calcularDigito(cpf, 10, 11);
        return digito2 == charParaDigito(cpf.charAt(10));
    }

    private static int calcularDigito(String cpf, int tamanho, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < tamanho; i++) {
            soma += charParaDigito(cpf.charAt(i)) * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int charParaDigito(char c) {
        return c - '0';
    }
}
