package br.com.lupainsights.dto;

public class ResendVerificationResponse {

    private String mensagem;

    public ResendVerificationResponse() {
    }

    public ResendVerificationResponse(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
