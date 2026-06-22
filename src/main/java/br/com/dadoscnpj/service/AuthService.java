package br.com.dadoscnpj.service;

import br.com.dadoscnpj.dto.AuthResponse;
import br.com.dadoscnpj.dto.LoginRequest;
import br.com.dadoscnpj.dto.RegisterRequest;
import br.com.dadoscnpj.dto.UserResponse;
import br.com.dadoscnpj.entity.UserEntity;
import br.com.dadoscnpj.repository.UserRepository;
import br.com.dadoscnpj.util.CpfValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse registrar(RegisterRequest request) {
        validarRegistro(request);

        String email = normalizarEmail(request.getEmail());
        String cpf = CpfValidator.removerMascara(request.getCpf());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
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

        userRepository.save(user);
        return montarAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Informe e-mail e senha");
        }

        String email = normalizarEmail(request.getEmail());
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos");
        }

        return montarAuthResponse(user);
    }

    public UserResponse obterUsuario(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return toUserResponse(user);
    }

    private void validarRegistro(RegisterRequest request) {
        if (request.getNome() == null || request.getNome().trim().length() < 3) {
            throw new IllegalArgumentException("Nome deve ter pelo menos 3 caracteres");
        }
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            throw new IllegalArgumentException("E-mail inválido");
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
        return new AuthResponse(token, toUserResponse(user));
    }

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getNome(), user.getEmail(), user.getCpf());
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
