package br.com.lupainsights.dto;

public class RegisterResponse {

    private String mensagem;
    private String email;

    public RegisterResponse() {
    }

    public RegisterResponse(String mensagem, String email) {
        this.mensagem = mensagem;
        this.email = email;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
