package br.com.lupainsights.service;

import br.com.lupainsights.config.AdminBootstrapProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.AuthResponse;
import br.com.lupainsights.dto.BootstrapAdminRequest;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.util.CpfValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthService authService;
    private final AdminBootstrapProperties bootstrapProperties;

    public AdminBootstrapService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 AuthService authService,
                                 AdminBootstrapProperties bootstrapProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authService = authService;
        this.bootstrapProperties = bootstrapProperties;
    }

    @Transactional
    public AuthResponse bootstrap(String secretHeader, BootstrapAdminRequest request) {
        validarSegredo(secretHeader);

        String email = normalizarEmail(request.getEmail());
        var usuarioExistente = userRepository.findByEmailIgnoreCase(email);

        if (usuarioExistente.isPresent()) {
            return promoverParaAdmin(usuarioExistente.get(), request);
        }
        return criarAdmin(email, request);
    }

    private AuthResponse promoverParaAdmin(UserEntity user, BootstrapAdminRequest request) {
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("Este usuário já é administrador");
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 8) {
                throw new IllegalArgumentException("Senha deve ter pelo menos 8 caracteres");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getNome() != null && !request.getNome().isBlank()) {
            if (request.getNome().trim().length() < 3) {
                throw new IllegalArgumentException("Nome deve ter pelo menos 3 caracteres");
            }
            user.setNome(request.getNome().trim());
        }

        user.setRole(UserRole.ADMIN);
        user.setPlan(SubscriptionPlan.PRO_PLUS);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return montarAuthResponse(user);
    }

    private AuthResponse criarAdmin(String email, BootstrapAdminRequest request) {
        validarCamposCriacao(request);

        String cpf = CpfValidator.removerMascara(request.getCpf());
        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setNome(request.getNome().trim());
        user.setEmail(email);
        user.setCpf(cpf);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(Instant.now());
        user.setEnabled(true);
        user.setRole(UserRole.ADMIN);
        user.setPlan(SubscriptionPlan.PRO_PLUS);
        userRepository.save(user);

        return montarAuthResponse(user);
    }

    private void validarSegredo(String secretHeader) {
        if (!bootstrapProperties.isEnabled()) {
            throw new IllegalStateException("Bootstrap de administrador não está habilitado");
        }
        if (secretHeader == null || secretHeader.isBlank()) {
            throw new IllegalArgumentException("Segredo de bootstrap inválido");
        }
        byte[] esperado = bootstrapProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        byte[] recebido = secretHeader.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperado, recebido)) {
            throw new IllegalArgumentException("Segredo de bootstrap inválido");
        }
    }

    private void validarCamposCriacao(BootstrapAdminRequest request) {
        if (request.getNome() == null || request.getNome().trim().length() < 3) {
            throw new IllegalArgumentException("Nome deve ter pelo menos 3 caracteres");
        }
        String erroCpf = CpfValidator.validar(request.getCpf());
        if (erroCpf != null) {
            throw new IllegalArgumentException(erroCpf);
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Senha deve ter pelo menos 8 caracteres");
        }
    }

    private AuthResponse montarAuthResponse(UserEntity user) {
        String token = jwtService.gerarToken(user.getId(), user.getEmail());
        return new AuthResponse(token, authService.obterUsuario(user.getId()));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
