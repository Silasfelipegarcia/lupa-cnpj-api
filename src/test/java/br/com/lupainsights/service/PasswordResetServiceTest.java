package br.com.lupainsights.service;

import br.com.lupainsights.config.EmailProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.ForgotPasswordResponse;
import br.com.lupainsights.dto.ResetPasswordRequest;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.util.PasswordResetTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSender emailSender;

    private PasswordEncoder passwordEncoder;
    private EmailProperties emailProperties;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        emailProperties = new EmailProperties();
        emailProperties.setFrontendUrl("http://localhost:4200");
        emailProperties.setResetTokenTtlHours(1);
        service = new PasswordResetService(userRepository, passwordEncoder, emailSender, emailProperties);
    }

    @Test
    void solicitarResetComEmailExistenteDeveGerarTokenEEnviarEmail() {
        UserEntity user = usuario("user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ForgotPasswordResponse response = service.solicitarReset("User@Example.com");

        assertNotNull(response.getMensagem());
        assertNotNull(user.getPasswordResetTokenHash());
        assertNotNull(user.getPasswordResetExpiresAt());
        assertTrue(user.getPasswordResetExpiresAt().isAfter(Instant.now()));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).enviarResetSenha(eq("user@example.com"), eq("Usuario Teste"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().startsWith("http://localhost:4200/redefinir-senha?token="));
    }

    @Test
    void solicitarResetComEmailInexistenteDeveRetornar200SemEnviarEmail() {
        when(userRepository.findByEmailIgnoreCase("nao@existe.com")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = service.solicitarReset("nao@existe.com");

        assertNotNull(response.getMensagem());
        verifyNoInteractions(emailSender);
        verify(userRepository, never()).save(any());
    }

    @Test
    void redefinirSenhaComTokenValidoDeveAtualizarSenhaEInvalidarToken() {
        String token = PasswordResetTokenUtil.gerarToken();
        UserEntity user = usuario("user@example.com");
        user.setPasswordResetTokenHash(PasswordResetTokenUtil.hashToken(token));
        user.setPasswordResetExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByPasswordResetTokenHash(PasswordResetTokenUtil.hashToken(token)))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setSenhaNova("novaSenha123");

        service.redefinirSenha(request);

        assertTrue(passwordEncoder.matches("novaSenha123", user.getPasswordHash()));
        assertNull(user.getPasswordResetTokenHash());
        assertNull(user.getPasswordResetExpiresAt());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void redefinirSenhaComTokenInvalidoDeveFalhar() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(PasswordResetTokenUtil.gerarToken());
        request.setSenhaNova("novaSenha123");
        when(userRepository.findByPasswordResetTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.redefinirSenha(request));
    }

    @Test
    void redefinirSenhaComTokenExpiradoDeveFalharELimparToken() {
        String token = PasswordResetTokenUtil.gerarToken();
        UserEntity user = usuario("user@example.com");
        user.setPasswordResetTokenHash(PasswordResetTokenUtil.hashToken(token));
        user.setPasswordResetExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        when(userRepository.findByPasswordResetTokenHash(PasswordResetTokenUtil.hashToken(token)))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setSenhaNova("novaSenha123");

        assertThrows(IllegalArgumentException.class, () -> service.redefinirSenha(request));
        assertNull(user.getPasswordResetTokenHash());
        assertNull(user.getPasswordResetExpiresAt());
    }

    @Test
    void redefinirSenhaReutilizandoTokenDeveFalhar() {
        String token = PasswordResetTokenUtil.gerarToken();
        when(userRepository.findByPasswordResetTokenHash(PasswordResetTokenUtil.hashToken(token)))
                .thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setSenhaNova("novaSenha123");

        assertThrows(IllegalArgumentException.class, () -> service.redefinirSenha(request));
    }

    private UserEntity usuario(String email) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setNome("Usuario Teste");
        user.setEmail(email);
        user.setCpf("12345678901");
        user.setPasswordHash(passwordEncoder.encode("senhaAntiga123"));
        user.setCreatedAt(Instant.now());
        user.setEnabled(true);
        user.setRole(UserRole.USER);
        user.setPlan(SubscriptionPlan.FREE);
        return user;
    }
}
