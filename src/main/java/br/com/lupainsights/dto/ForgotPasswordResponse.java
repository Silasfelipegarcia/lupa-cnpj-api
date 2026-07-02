package br.com.lupainsights.dto;

public class ForgotPasswordResponse {

    private final String mensagem;

    public ForgotPasswordResponse(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}
