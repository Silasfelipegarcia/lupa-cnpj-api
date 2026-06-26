package br.com.lupainsights.controller;

import br.com.lupainsights.audit.AuditAction;
import br.com.lupainsights.audit.AuditLogService;
import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.dto.AdminOverviewResponse;
import br.com.lupainsights.dto.AdminUserDetailResponse;
import br.com.lupainsights.dto.AdminUsersPageResponse;
import br.com.lupainsights.observability.RequestContext;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.service.AdminMetricsService;
import br.com.lupainsights.util.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminMetricsService adminMetricsService;
    private final AuditLogService auditLogService;
    private final RequestIpResolver requestIpResolver;

    public AdminController(AdminMetricsService adminMetricsService,
                           AuditLogService auditLogService,
                           RequestIpResolver requestIpResolver) {
        this.adminMetricsService = adminMetricsService;
        this.auditLogService = auditLogService;
        this.requestIpResolver = requestIpResolver;
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> overview(@RequestParam(defaultValue = "30") int days,
                                                          HttpServletRequest request) {
        logAccess(request, "/admin/overview");
        return ResponseEntity.ok(adminMetricsService.overview(days));
    }

    @GetMapping("/users")
    public ResponseEntity<AdminUsersPageResponse> users(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) SubscriptionPlan plan,
                                                        @RequestParam(required = false) String q,
                                                        HttpServletRequest request) {
        logAccess(request, "/admin/users");
        return ResponseEntity.ok(adminMetricsService.listUsers(page, size, plan, q));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailResponse> userDetail(@PathVariable UUID id,
                                                              HttpServletRequest request) {
        logAccess(request, "/admin/users/" + id);
        return ResponseEntity.ok(adminMetricsService.userDetail(id));
    }

    private void logAccess(HttpServletRequest request, String path) {
        auditLogService.registrar(
                AuditAction.ADMIN_ACCESS,
                "GET",
                path,
                requestIpResolver.resolver(request),
                200,
                SecurityUtils.currentUserIdOrNull(),
                null,
                0,
                RequestContext.requestIdAtual());
    }
}
