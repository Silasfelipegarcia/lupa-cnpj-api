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
        String assunto = "Redefinição de senha — Lupa Insights";
        String html = montarHtmlReset(nome, resetLink);
        String texto = montarTextoReset(nome, resetLink);

        if (!emailProperties.isEnabled() || resend == null) {
            log.info("E-mail de reset (dev/log): destinatario={} link={}", email, resetLink);
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
            log.info("E-mail de reset enviado para {} (id={})", email, response.getId());
        } catch (ResendException e) {
            log.error("Falha ao enviar e-mail de reset para {}: {}", email, e.getMessage());
            throw new IllegalStateException("Não foi possível enviar o e-mail de redefinição. Tente novamente mais tarde.");
        }
    }

    private String montarHtmlReset(String nome, String resetLink) {
        String saudacao = nome != null && !nome.isBlank() ? "Olá, " + escapeHtml(nome.trim()) + "." : "Olá.";
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <body style="font-family: Arial, sans-serif; line-height: 1.5; color: #111;">
                  <p>%s</p>
                  <p>Recebemos uma solicitação para redefinir a senha da sua conta no Lupa Insights.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;">Redefinir senha</a></p>
                  <p>Ou copie e cole este link no navegador:<br><a href="%s">%s</a></p>
                  <p>Este link expira em 1 hora e só pode ser usado uma vez.</p>
                  <p>Se você não solicitou a redefinição, ignore este e-mail.</p>
                  <p>— Equipe Lupa Insights</p>
                </body>
                </html>
                """.formatted(saudacao, resetLink, resetLink, resetLink);
    }

    private String montarTextoReset(String nome, String resetLink) {
        String saudacao = nome != null && !nome.isBlank() ? "Olá, " + nome.trim() + "." : "Olá.";
        return """
                %s

                Recebemos uma solicitação para redefinir a senha da sua conta no Lupa Insights.

                Acesse o link abaixo para definir uma nova senha:
                %s

                Este link expira em 1 hora e só pode ser usado uma vez.

                Se você não solicitou a redefinição, ignore este e-mail.

                — Equipe Lupa Insights
                """.formatted(saudacao, resetLink);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
