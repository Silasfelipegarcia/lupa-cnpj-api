package br.com.dadoscnpj.controller;

import br.com.dadoscnpj.audit.AuditAction;
import br.com.dadoscnpj.audit.AuditLogService;
import br.com.dadoscnpj.dto.AnalyticsEventRequest;
import br.com.dadoscnpj.security.SecurityUtils;
import br.com.dadoscnpj.util.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AuditLogService auditLogService;

    public AnalyticsController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping("/event")
    public ResponseEntity<Void> registrarEvento(@RequestBody AnalyticsEventRequest body,
                                                HttpServletRequest request) {
        if (body.getEvent() == null || body.getEvent().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId = null;
        try {
            userId = SecurityUtils.currentUserId();
        } catch (Exception ignored) {
            // visitante
        }

        String detalhes = body.getProperties() != null ? body.getProperties() : "";
        auditLogService.registrar(
                AuditAction.PRODUCT_EVENT,
                "POST",
                "/analytics/event:" + body.getEvent(),
                RequestIpResolver.resolver(request),
                200,
                userId,
                detalhes,
                0);
        return ResponseEntity.accepted().build();
    }
}
