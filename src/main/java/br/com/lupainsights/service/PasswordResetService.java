package br.com.lupainsights.service;

import br.com.lupainsights.config.EmailProperties;
import br.com.lupainsights.dto.ForgotPasswordResponse;
import br.com.lupainsights.dto.ResetPasswordRequest;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.util.PasswordResetTokenUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final String MENSAGEM_GENERICA =
            "Se o e-mail estiver cadastrado, você receberá instruções para redefinir sua senha em instantes.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final EmailProperties emailProperties;

    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailSender emailSender,
                                EmailProperties emailProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.emailProperties = emailProperties;
    }

    @Transactional
    public ForgotPasswordResponse solicitarReset(String email) {
        String emailNormalizado = normalizarEmail(email);
        Optional<UserEntity> usuarioOpt = userRepository.findByEmailIgnoreCase(emailNormalizado);

        if (usuarioOpt.isEmpty()) {
            return new ForgotPasswordResponse(MENSAGEM_GENERICA);
        }

        UserEntity user = usuarioOpt.get();
        if (!user.isEnabled()) {
            return new ForgotPasswordResponse(MENSAGEM_GENERICA);
        }

        String token = PasswordResetTokenUtil.gerarToken();
        user.setPasswordResetTokenHash(PasswordResetTokenUtil.hashToken(token));
        user.setPasswordResetExpiresAt(Instant.now().plus(
                emailProperties.getResetTokenTtlHours(), ChronoUnit.HOURS));
        userRepository.save(user);

        String resetLink = montarResetLink(token);
        emailSender.enviarResetSenha(user.getEmail(), user.getNome(), resetLink);

        return new ForgotPasswordResponse(MENSAGEM_GENERICA);
    }

    @Transactional
    public void redefinirSenha(ResetPasswordRequest request) {
        if (request.getSenhaNova() == null || request.getSenhaNova().length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres");
        }

        String tokenHash = PasswordResetTokenUtil.hashToken(request.getToken().trim());
        UserEntity user = userRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Link de redefinição inválido ou expirado"));

        if (!user.isPasswordResetTokenValido()) {
            limparTokenReset(user);
            userRepository.save(user);
            throw new IllegalArgumentException("Link de redefinição inválido ou expirado");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getSenhaNova()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        limparTokenReset(user);
        userRepository.save(user);
    }

    private void limparTokenReset(UserEntity user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
    }

    private String montarResetLink(String token) {
        String base = emailProperties.getFrontendUrl().replaceAll("/$", "");
        return base + "/redefinir-senha?token=" + token;
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
