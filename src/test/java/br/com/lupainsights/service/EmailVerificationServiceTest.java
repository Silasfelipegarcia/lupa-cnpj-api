package br.com.lupainsights.service;

import br.com.lupainsights.config.EmailProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.AuthResponse;
import br.com.lupainsights.dto.ResendVerificationResponse;
import br.com.lupainsights.dto.UserResponse;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.util.PasswordResetTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private AuthService authService;

    @Mock
    private TrialService trialService;

    private EmailProperties emailProperties;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setFrontendUrl("http://localhost:4200");
        emailProperties.setVerificationTokenTtlHours(24);
        service = new EmailVerificationService(userRepository, emailSender, emailProperties, authService, trialService);
    }

    @Test
    void enviarVerificacaoDeveGerarTokenEEnviarEmail() {
        UserEntity user = usuario("user@example.com", false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.enviarVerificacao(user);

        assertNotNull(user.getEmailVerificationTokenHash());
        assertNotNull(user.getEmailVerificationExpiresAt());
        assertTrue(user.getEmailVerificationExpiresAt().isAfter(Instant.now()));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).enviarVerificacaoEmail(eq("user@example.com"), eq("Usuario Teste"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().startsWith("http://localhost:4200/verificar-email?token="));
    }

    @Test
    void verificarComTokenValidoDeveMarcarEmailVerificado() {
        String token = PasswordResetTokenUtil.gerarToken();
        UserEntity user = usuario("user@example.com", false);
        user.setEmailVerificationTokenHash(PasswordResetTokenUtil.hashToken(token));
        user.setEmailVerificationExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByEmailVerificationTokenHash(PasswordResetTokenUtil.hashToken(token)))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse userResponse = new UserResponse(user.getId(), user.getNome(), user.getEmail(), user.getCpf());
        when(authService.montarAuthResponsePublico(user)).thenReturn(new AuthResponse("jwt", userResponse));

        AuthResponse response = service.verificar(token);

        verify(trialService).ativarTrialInicialSeElegivel(user);

        assertTrue(user.isEmailVerified());
        assertNull(user.getEmailVerificationTokenHash());
        assertNull(user.getEmailVerificationExpiresAt());
        assertEquals("jwt", response.getToken());
    }

    @Test
    void verificarComTokenInvalidoDeveFalhar() {
        assertThrows(IllegalArgumentException.class, () -> service.verificar("token-invalido"));
    }

    @Test
    void reenviarParaUsuarioNaoVerificadoDeveEnviarEmail() {
        UserEntity user = usuario("user@example.com", false);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResendVerificationResponse response = service.reenviar("user@example.com");

        assertNotNull(response.getMensagem());
        verify(emailSender).enviarVerificacaoEmail(any(), any(), any());
    }

    @Test
    void reenviarParaUsuarioJaVerificadoNaoDeveEnviarEmail() {
        UserEntity user = usuario("user@example.com", true);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        service.reenviar("user@example.com");

        verifyNoInteractions(emailSender);
    }

    @Test
    void reenviarParaEmailInexistenteNaoDeveEnviarEmail() {
        when(userRepository.findByEmailIgnoreCase("nao@existe.com")).thenReturn(Optional.empty());

        ResendVerificationResponse response = service.reenviar("nao@existe.com");

        assertNotNull(response.getMensagem());
        verifyNoInteractions(emailSender);
    }

    private UserEntity usuario(String email, boolean emailVerified) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setNome("Usuario Teste");
        user.setEmail(email);
        user.setCpf("12345678901");
        user.setPasswordHash("hash");
        user.setCreatedAt(Instant.now());
        user.setEnabled(true);
        user.setEmailVerified(emailVerified);
        user.setRole(UserRole.USER);
        user.setPlan(SubscriptionPlan.FREE);
        return user;
    }
}
