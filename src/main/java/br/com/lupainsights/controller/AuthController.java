package br.com.lupainsights.controller;

import br.com.lupainsights.audit.AuditAction;
import br.com.lupainsights.audit.AuditLogService;
import br.com.lupainsights.dto.AuthResponse;
import br.com.lupainsights.dto.BootstrapAdminRequest;
import br.com.lupainsights.dto.ChangePasswordRequest;
import br.com.lupainsights.dto.ForgotPasswordRequest;
import br.com.lupainsights.dto.ForgotPasswordResponse;
import br.com.lupainsights.dto.LoginRequest;
import br.com.lupainsights.dto.RegisterRequest;
import br.com.lupainsights.dto.RegisterResponse;
import br.com.lupainsights.dto.ResendVerificationRequest;
import br.com.lupainsights.dto.ResendVerificationResponse;
import br.com.lupainsights.dto.ResetPasswordRequest;
import br.com.lupainsights.dto.VerifyEmailRequest;
import br.com.lupainsights.dto.UserResponse;
import br.com.lupainsights.observability.RequestContext;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.service.AdminBootstrapService;
import br.com.lupainsights.service.AuthService;
import br.com.lupainsights.service.EmailVerificationService;
import br.com.lupainsights.service.PasswordResetService;
import br.com.lupainsights.util.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public static final String ADMIN_BOOTSTRAP_SECRET_HEADER = "X-Admin-Bootstrap-Secret";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final AdminBootstrapService adminBootstrapService;
    private final AuditLogService auditLogService;
    private final RequestIpResolver requestIpResolver;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          EmailVerificationService emailVerificationService,
                          AdminBootstrapService adminBootstrapService,
                          AuditLogService auditLogService,
                          RequestIpResolver requestIpResolver) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
        this.adminBootstrapService = adminBootstrapService;
        this.auditLogService = auditLogService;
        this.requestIpResolver = requestIpResolver;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registrar(@Valid @RequestBody RegisterRequest request,
                                                      HttpServletRequest httpRequest) {
        RegisterResponse response = authService.registrar(request);
        auditLogService.registrar(
                AuditAction.EMAIL_VERIFICATION_SENT,
                "POST",
                "/auth/register",
                requestIpResolver.resolver(httpRequest),
                201,
                null,
                null,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verificarEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                     HttpServletRequest httpRequest) {
        AuthResponse response = emailVerificationService.verificar(request.getToken());
        auditLogService.registrar(
                AuditAction.EMAIL_VERIFICATION_COMPLETE,
                "POST",
                "/auth/verify-email",
                requestIpResolver.resolver(httpRequest),
                200,
                response.getUser().getId(),
                null,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ResendVerificationResponse> reenviarVerificacao(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {
        ResendVerificationResponse response = emailVerificationService.reenviar(request.getEmail());
        auditLogService.registrar(
                AuditAction.EMAIL_VERIFICATION_SENT,
                "POST",
                "/auth/resend-verification",
                requestIpResolver.resolver(httpRequest),
                200,
                null,
                null,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> esqueciSenha(@Valid @RequestBody ForgotPasswordRequest request,
                                                               HttpServletRequest httpRequest) {
        ForgotPasswordResponse response = passwordResetService.solicitarReset(request.getEmail());
        auditLogService.registrar(
                AuditAction.PASSWORD_RESET_REQUEST,
                "POST",
                "/auth/forgot-password",
                requestIpResolver.resolver(httpRequest),
                200,
                null,
                null,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<AuthResponse> bootstrapAdmin(@Valid @RequestBody BootstrapAdminRequest request,
                                                     @RequestHeader(value = ADMIN_BOOTSTRAP_SECRET_HEADER, required = false)
                                                     String bootstrapSecret,
                                                     HttpServletRequest httpRequest) {
        AuthResponse response = adminBootstrapService.bootstrap(bootstrapSecret, request);
        auditLogService.registrar(
                AuditAction.ADMIN_BOOTSTRAP,
                "POST",
                "/auth/bootstrap-admin",
                requestIpResolver.resolver(httpRequest),
                201,
                response.getUser().getId(),
                null,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody ResetPasswordRequest request,
                                               HttpServletRequest httpRequest) {
        passwordResetService.redefinirSenha(request);
        auditLogService.registrar(
                AuditAction.PASSWORD_RESET_COMPLETE,
                "POST",
                "/auth/reset-password",
                requestIpResolver.resolver(httpRequest),
                204,
                null,
                null,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.obterUsuario(SecurityUtils.currentUserId()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody ChangePasswordRequest request,
                                           HttpServletRequest httpRequest) {
        UUID userId = SecurityUtils.currentUserId();
        authService.alterarSenha(userId, request);
        auditLogService.registrar(
                AuditAction.PASSWORD_CHANGE,
                "PUT",
                "/auth/password",
                requestIpResolver.resolver(httpRequest),
                204,
                userId,
                null,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.noContent().build();
    }
}
