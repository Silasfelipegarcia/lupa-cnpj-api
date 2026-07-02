package br.com.lupainsights.service;

import br.com.lupainsights.config.SecurityProperties;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.AuthResponse;
import br.com.lupainsights.dto.ChangePasswordRequest;
import br.com.lupainsights.dto.LoginRequest;
import br.com.lupainsights.dto.RegisterRequest;
import br.com.lupainsights.dto.RegisterResponse;
import br.com.lupainsights.dto.UserResponse;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.plan.PlanLimitsService;
import br.com.lupainsights.plan.PlanService;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.subscription.SubscriptionService;
import br.com.lupainsights.util.CpfValidator;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlanService planService;
    private final PlanLimitsService planLimitsService;
    private final TrialService trialService;
    private final SubscriptionService subscriptionService;
    private final SecurityProperties securityProperties;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PlanService planService,
                       PlanLimitsService planLimitsService,
                       TrialService trialService,
                       SubscriptionService subscriptionService,
                       SecurityProperties securityProperties,
                       @Lazy EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.planService = planService;
        this.planLimitsService = planLimitsService;
        this.trialService = trialService;
        this.subscriptionService = subscriptionService;
        this.securityProperties = securityProperties;
        this.emailVerificationService = emailVerificationService;
    }

    public RegisterResponse registrar(RegisterRequest request) {
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
        user.setEmailVerified(false);
        user.setRole(UserRole.USER);
        user.setPlan(SubscriptionPlan.FREE);

        userRepository.save(user);
        emailVerificationService.enviarVerificacao(user);
        return new RegisterResponse(emailVerificationService.mensagemCadastro(), email);
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Informe e-mail e senha");
        }

        String email = normalizarEmail(request.getEmail());
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos"));

        if (user.isContaBloqueada()) {
            throw new IllegalStateException("Conta temporariamente bloqueada por tentativas de login. Tente novamente mais tarde.");
        }

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registrarFalhaLogin(user);
            throw new IllegalArgumentException("E-mail ou senha inválidos");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalStateException(
                    "Confirme seu e-mail antes de entrar. Acesse o link enviado no cadastro ou solicite um novo em /cadastro-pendente.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        trialService.expirarTrialSeNecessario(user);
        subscriptionService.expirarSeNecessario(user);
        return montarAuthResponse(user);
    }

    public UserResponse obterUsuario(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        subscriptionService.expirarSeNecessario(user);
        trialService.expirarTrialSeNecessario(user);
        return toUserResponse(user);
    }

    @Transactional
    public void alterarSenha(UUID userId, ChangePasswordRequest request) {
        if (request.getSenhaAtual() == null || request.getSenhaAtual().isBlank()) {
            throw new IllegalArgumentException("Informe a senha atual");
        }
        if (request.getSenhaNova() == null || request.getSenhaNova().length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres");
        }
        if (request.getSenhaNova().equals(request.getSenhaAtual())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da atual");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.getSenhaAtual(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getSenhaNova()));
        userRepository.save(user);
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

    public AuthResponse montarAuthResponsePublico(UserEntity user) {
        trialService.expirarTrialSeNecessario(user);
        subscriptionService.expirarSeNecessario(user);
        return montarAuthResponse(user);
    }

    private AuthResponse montarAuthResponse(UserEntity user) {
        String token = jwtService.gerarToken(user.getId(), user.getEmail());
        return new AuthResponse(token, toUserResponse(user));
    }

    private UserResponse toUserResponse(UserEntity user) {
        UserResponse response = new UserResponse(user.getId(), user.getNome(), user.getEmail(), user.getCpf());
        response.setCreatedAt(user.getCreatedAt());
        response.setRole(user.getRole());
        SubscriptionPlan planoEfetivo = subscriptionService.resolverPlanoEfetivo(user);
        response.setPlan(planoEfetivo);
        response.setPlanNome(planLimitsService.isMaster(user)
                ? "Master"
                : planLimitsService.nomeExibicao(planoEfetivo));
        response.setUsage(planService.montarUsage(user));
        response.setSubscription(subscriptionService.montarStatus(user));
        return response;
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void registrarFalhaLogin(UserEntity user) {
        int tentativas = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(tentativas);
        if (tentativas >= securityProperties.getLoginFailuresBeforeLock()) {
            user.setLockedUntil(Instant.now().plus(
                    securityProperties.getLoginLockMinutes(), ChronoUnit.MINUTES));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }
}
