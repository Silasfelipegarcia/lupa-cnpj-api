package br.com.lupainsights.service;

import br.com.lupainsights.config.EmailProperties;
import br.com.lupainsights.dto.AuthResponse;
import br.com.lupainsights.dto.ResendVerificationResponse;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.util.PasswordResetTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private static final String MENSAGEM_REENVIO =
            "Se o e-mail estiver cadastrado e ainda não foi confirmado, você receberá um novo link em instantes.";

    private static final String MENSAGEM_CADASTRO =
            "Cadastro realizado. Verifique seu e-mail para confirmar a conta antes de entrar.";

    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final EmailProperties emailProperties;
    private final AuthService authService;

    public EmailVerificationService(UserRepository userRepository,
                                  EmailSender emailSender,
                                  EmailProperties emailProperties,
                                  @Lazy AuthService authService) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.emailProperties = emailProperties;
        this.authService = authService;
    }

    @Transactional
    public void enviarVerificacao(UserEntity user) {
        String token = PasswordResetTokenUtil.gerarToken();
        user.setEmailVerificationTokenHash(PasswordResetTokenUtil.hashToken(token));
        user.setEmailVerificationExpiresAt(Instant.now().plus(
                emailProperties.getVerificationTokenTtlHours(), ChronoUnit.HOURS));
        userRepository.save(user);

        String verifyLink = montarVerifyLink(token);
        try {
            emailSender.enviarVerificacaoEmail(user.getEmail(), user.getNome(), verifyLink);
        } catch (RuntimeException ex) {
            log.error("Falha ao enviar verificação para {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    @Transactional
    public AuthResponse verificar(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Link de verificação inválido ou expirado");
        }

        String tokenHash = PasswordResetTokenUtil.hashToken(rawToken.trim());
        UserEntity user = userRepository.findByEmailVerificationTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Link de verificação inválido ou expirado"));

        if (!user.isEmailVerificationTokenValido()) {
            limparTokenVerificacao(user);
            userRepository.save(user);
            throw new IllegalArgumentException("Link de verificação inválido ou expirado");
        }

        user.setEmailVerified(true);
        limparTokenVerificacao(user);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return authService.montarAuthResponsePublico(user);
    }

    @Transactional
    public ResendVerificationResponse reenviar(String email) {
        String emailNormalizado = normalizarEmail(email);
        Optional<UserEntity> usuarioOpt = userRepository.findByEmailIgnoreCase(emailNormalizado);

        if (usuarioOpt.isEmpty()) {
            return new ResendVerificationResponse(MENSAGEM_REENVIO);
        }

        UserEntity user = usuarioOpt.get();
        if (!user.isEnabled() || user.isEmailVerified()) {
            return new ResendVerificationResponse(MENSAGEM_REENVIO);
        }

        enviarVerificacao(user);
        return new ResendVerificationResponse(MENSAGEM_REENVIO);
    }

    public String mensagemCadastro() {
        return MENSAGEM_CADASTRO;
    }

    private void limparTokenVerificacao(UserEntity user) {
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
    }

    private String montarVerifyLink(String token) {
        String base = emailProperties.getFrontendUrl().replaceAll("/$", "");
        return base + "/verificar-email?token=" + token;
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
