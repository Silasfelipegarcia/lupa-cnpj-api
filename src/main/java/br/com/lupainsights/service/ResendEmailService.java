package br.com.lupainsights.service;

import br.com.lupainsights.config.EmailProperties;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final EmailProperties emailProperties;
    private final Resend resend;

    public ResendEmailService(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
        this.resend = emailProperties.isResendConfigured()
                ? new Resend(emailProperties.getResendApiKey())
                : null;
    }

    @Override
    public void enviarResetSenha(String email, String nome, String resetLink) {
        String ttlLabel = formatarTtlHoras(emailProperties.getResetTokenTtlHours());
        String assunto = "Redefinição de senha — Lupa Insights";
        dispatch(
                email,
                assunto,
                "reset",
                montarHtmlReset(nome, resetLink, ttlLabel),
                montarTextoReset(nome, resetLink, ttlLabel),
                resetLink);
    }

    @Override
    public void enviarVerificacaoEmail(String email, String nome, String verifyLink) {
        String ttlLabel = formatarTtlHoras(emailProperties.getVerificationTokenTtlHours());
        String assunto = "Confirme seu e-mail — Lupa Insights";
        dispatch(
                email,
                assunto,
                "verify",
                montarHtmlVerificacao(nome, verifyLink, ttlLabel),
                montarTextoVerificacao(nome, verifyLink, ttlLabel),
                verifyLink);
    }

    private void dispatch(String email, String assunto, String tipo, String html, String texto, String link) {
        if (!emailProperties.isEnabled() || resend == null) {
            log.info("E-mail {} (dev/log): destinatario={} link={}", tipo, email, link);
            return;
        }

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(emailProperties.formatFromAddress())
                .to(email)
                .subject(assunto)
                .html(html)
                .text(texto)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            log.info("E-mail {} enviado para {} (id={})", tipo, email, response.getId());
        } catch (ResendException e) {
            log.error("Falha ao enviar e-mail {} para {}: {}", tipo, email, e.getMessage());
            throw new IllegalStateException("Não foi possível enviar o e-mail. Tente novamente mais tarde.");
        }
    }

    private String formatarTtlHoras(int horas) {
        return horas == 1 ? "1 hora" : horas + " horas";
    }

    private String montarHtmlReset(String nome, String resetLink, String ttlLabel) {
        String saudacao = saudacaoHtml(nome);
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <body style="font-family: Arial, sans-serif; line-height: 1.5; color: #111;">
                  <p>%s</p>
                  <p>Recebemos uma solicitação para redefinir a senha da sua conta no Lupa Insights.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;">Redefinir senha</a></p>
                  <p>Ou copie e cole este link no navegador:<br><a href="%s">%s</a></p>
                  <p>Este link expira em %s e só pode ser usado uma vez.</p>
                  <p>Se você não solicitou a redefinição, ignore este e-mail.</p>
                  <p>— Equipe Lupa Insights</p>
                </body>
                </html>
                """.formatted(saudacao, resetLink, resetLink, resetLink, ttlLabel);
    }

    private String montarTextoReset(String nome, String resetLink, String ttlLabel) {
        return """
                %s

                Recebemos uma solicitação para redefinir a senha da sua conta no Lupa Insights.

                Acesse o link abaixo para definir uma nova senha:
                %s

                Este link expira em %s e só pode ser usado uma vez.

                Se você não solicitou a redefinição, ignore este e-mail.

                — Equipe Lupa Insights
                """.formatted(saudacaoTexto(nome), resetLink, ttlLabel);
    }

    private String montarHtmlVerificacao(String nome, String verifyLink, String ttlLabel) {
        String saudacao = saudacaoHtml(nome);
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <body style="font-family: Arial, sans-serif; line-height: 1.5; color: #111;">
                  <p>%s</p>
                  <p>Obrigado por se cadastrar no Lupa Insights. Confirme seu e-mail para ativar sua conta.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;">Confirmar e-mail</a></p>
                  <p>Ou copie e cole este link no navegador:<br><a href="%s">%s</a></p>
                  <p>Este link expira em %s.</p>
                  <p>Se você não criou esta conta, ignore este e-mail.</p>
                  <p>— Equipe Lupa Insights</p>
                </body>
                </html>
                """.formatted(saudacao, verifyLink, verifyLink, verifyLink, ttlLabel);
    }

    private String montarTextoVerificacao(String nome, String verifyLink, String ttlLabel) {
        return """
                %s

                Obrigado por se cadastrar no Lupa Insights. Confirme seu e-mail para ativar sua conta.

                Acesse o link abaixo:
                %s

                Este link expira em %s.

                Se você não criou esta conta, ignore este e-mail.

                — Equipe Lupa Insights
                """.formatted(saudacaoTexto(nome), verifyLink, ttlLabel);
    }

    private String saudacaoHtml(String nome) {
        return nome != null && !nome.isBlank()
                ? "Olá, " + escapeHtml(nome.trim()) + "."
                : "Olá.";
    }

    private String saudacaoTexto(String nome) {
        return nome != null && !nome.isBlank() ? "Olá, " + nome.trim() + "." : "Olá.";
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
