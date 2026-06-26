package br.com.lupainsights.controller;

import br.com.lupainsights.audit.AuditAction;
import br.com.lupainsights.audit.AuditLogService;
import br.com.lupainsights.dto.AuthResponse;
import br.com.lupainsights.dto.ChangePasswordRequest;
import br.com.lupainsights.dto.LoginRequest;
import br.com.lupainsights.dto.RegisterRequest;
import br.com.lupainsights.dto.UserResponse;
import br.com.lupainsights.observability.RequestContext;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.service.AuthService;
import br.com.lupainsights.util.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final RequestIpResolver requestIpResolver;

    public AuthController(AuthService authService,
                          AuditLogService auditLogService,
                          RequestIpResolver requestIpResolver) {
        this.authService = authService;
        this.auditLogService = auditLogService;
        this.requestIpResolver = requestIpResolver;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
