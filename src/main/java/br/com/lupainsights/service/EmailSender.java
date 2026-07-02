package br.com.lupainsights.service;

public interface EmailSender {

    void enviarResetSenha(String email, String nome, String resetLink);

    void enviarVerificacaoEmail(String email, String nome, String verifyLink);
}
