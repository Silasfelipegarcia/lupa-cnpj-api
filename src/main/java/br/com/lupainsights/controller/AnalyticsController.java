package br.com.lupainsights.controller;

import br.com.lupainsights.audit.AuditAction;
import br.com.lupainsights.audit.AuditLogService;
import br.com.lupainsights.dto.AnalyticsEventRequest;
import br.com.lupainsights.observability.RequestContext;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.util.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final RequestIpResolver requestIpResolver;

    public AnalyticsController(AuditLogService auditLogService, RequestIpResolver requestIpResolver) {
        this.auditLogService = auditLogService;
        this.requestIpResolver = requestIpResolver;
    }

    @PostMapping("/event")
    public ResponseEntity<Void> registrarEvento(@Valid @RequestBody AnalyticsEventRequest body,
                                                HttpServletRequest request) {
        String properties = body.getProperties() != null ? body.getProperties() : "";

        UUID userId = null;
        try {
            userId = SecurityUtils.currentUserId();
        } catch (Exception ignored) {
            // visitante
        }

        auditLogService.registrar(
                AuditAction.PRODUCT_EVENT,
                "POST",
                "/analytics/event:" + body.getEvent(),
                requestIpResolver.resolver(request),
                200,
                userId,
                properties,
                0,
                RequestContext.requestIdAtual());
        return ResponseEntity.accepted().build();
    }
}
