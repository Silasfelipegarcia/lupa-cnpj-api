package br.com.lupainsights.controller;

import br.com.lupainsights.audit.AuditAction;
import br.com.lupainsights.audit.AuditLogService;
import br.com.lupainsights.dto.AnalyticsEventRequest;
import br.com.lupainsights.observability.RequestContext;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.util.RequestIpResolver;
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
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.accepted().build();
    }
}
