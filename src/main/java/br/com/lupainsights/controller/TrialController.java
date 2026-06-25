package br.com.lupainsights.controller;

import br.com.lupainsights.dto.UserResponse;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.service.AuthService;
import br.com.lupainsights.service.TrialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class TrialController {

    private final TrialService trialService;
    private final AuthService authService;

    public TrialController(TrialService trialService, AuthService authService) {
        this.trialService = trialService;
        this.authService = authService;
    }

    @PostMapping("/trial")
    public ResponseEntity<UserResponse> iniciarTrial() {
        UUID userId = SecurityUtils.currentUserId();
        trialService.iniciarTrial(userId);
        return ResponseEntity.ok(authService.obterUsuario(userId));
    }
}
