package br.com.dadoscnpj.controller;

import br.com.dadoscnpj.dto.UserResponse;
import br.com.dadoscnpj.security.SecurityUtils;
import br.com.dadoscnpj.service.AuthService;
import br.com.dadoscnpj.service.TrialService;
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
