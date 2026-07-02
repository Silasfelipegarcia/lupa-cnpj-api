package br.com.lupainsights.service;

import br.com.lupainsights.config.AdminBootstrapProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.AuthResponse;
import br.com.lupainsights.dto.BootstrapAdminRequest;
import br.com.lupainsights.dto.UserResponse;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    private static final String SECRET = "bootstrap-secret-forte-123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthService authService;

    private PasswordEncoder passwordEncoder;
    private AdminBootstrapProperties bootstrapProperties;
    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        bootstrapProperties = new AdminBootstrapProperties();
        bootstrapProperties.setSecret(SECRET);
        service = new AdminBootstrapService(
                userRepository, passwordEncoder, jwtService, authService, bootstrapProperties);
    }

    @Test
    void deveCriarNovoAdminQuandoEmailNaoExiste() {
        BootstrapAdminRequest request = new BootstrapAdminRequest();
        request.setNome("Silas Admin");
        request.setEmail("silas@example.com");
        request.setCpf("52998224725");
        request.setPassword("senha-forte-123");

        when(userRepository.findByEmailIgnoreCase("silas@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByCpf("52998224725")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.gerarToken(any(), any())).thenReturn("jwt-token");

        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "Silas Admin", "silas@example.com", "52998224725");
        userResponse.setRole(UserRole.ADMIN);
        when(authService.obterUsuario(any())).thenReturn(userResponse);

        AuthResponse response = service.bootstrap(SECRET, request);

        assertEquals("jwt-token", response.getToken());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity salvo = captor.getValue();
        assertEquals(UserRole.ADMIN, salvo.getRole());
        assertEquals(SubscriptionPlan.PRO_PLUS, salvo.getPlan());
        assertTrue(passwordEncoder.matches("senha-forte-123", salvo.getPasswordHash()));
    }

    @Test
    void devePromoverUsuarioExistenteParaAdmin() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setNome("Silas");
        user.setEmail("silas@example.com");
        user.setCpf("52998224725");
        user.setPasswordHash(passwordEncoder.encode("antiga-senha"));
        user.setRole(UserRole.USER);
        user.setPlan(SubscriptionPlan.FREE);
        user.setCreatedAt(Instant.now());
        user.setEnabled(true);

        BootstrapAdminRequest request = new BootstrapAdminRequest();
        request.setEmail("silas@example.com");
        request.setPassword("nova-senha-123");

        when(userRepository.findByEmailIgnoreCase("silas@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.gerarToken(any(), any())).thenReturn("jwt-token");

        UserResponse userResponse = new UserResponse(user.getId(), user.getNome(), user.getEmail(), user.getCpf());
        userResponse.setRole(UserRole.ADMIN);
        when(authService.obterUsuario(user.getId())).thenReturn(userResponse);

        AuthResponse response = service.bootstrap(SECRET, request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(SubscriptionPlan.PRO_PLUS, user.getPlan());
        assertTrue(passwordEncoder.matches("nova-senha-123", user.getPasswordHash()));
    }

    @Test
    void deveRejeitarSegredoInvalido() {
        BootstrapAdminRequest request = new BootstrapAdminRequest();
        request.setEmail("silas@example.com");

        assertThrows(IllegalArgumentException.class, () -> service.bootstrap("segredo-errado", request));
    }

    @Test
    void deveRejeitarQuandoBootstrapDesabilitado() {
        bootstrapProperties.setSecret("");
        BootstrapAdminRequest request = new BootstrapAdminRequest();
        request.setEmail("silas@example.com");

        assertThrows(IllegalStateException.class, () -> service.bootstrap(SECRET, request));
    }
}
