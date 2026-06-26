package br.com.lupainsights.audit;

import br.com.lupainsights.observability.RequestContext;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.util.RequestIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    public AuditLoggingFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long inicio = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            registrarSeNecessario(request, response, inicio);
        }
    }

    private void registrarSeNecessario(HttpServletRequest request,
                                       HttpServletResponse response,
                                       long inicio) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();
        int durationMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        String ip = RequestIpResolver.resolver(request);
        UUID userId = extrairUserId();

        AuditAction action = resolverAcao(method, path, status);
        if (action == null) {
            return;
        }

        String details = montarDetalhes(method, path, status);
        String requestId = RequestContext.requestIdAtual();
        auditLogService.registrar(action, method, path, ip, status, userId, details, durationMs, requestId);
    }

    private AuditAction resolverAcao(String method, String path, int status) {
        if (path.equals("/health") || path.equals("/")) {
            return null;
        }
        if (path.matches(".*/import/[^/]+/status$") || path.endsWith("/import/ativo")) {
            return null;
        }
        if (path.startsWith("/auth/login") && HttpMethod.POST.matches(method)) {
            return status >= 200 && status < 300
                    ? AuditAction.AUTH_LOGIN_SUCCESS
                    : AuditAction.AUTH_LOGIN_FAILURE;
        }
        if (path.startsWith("/auth/register") && HttpMethod.POST.matches(method)) {
            return status >= 200 && status < 300 ? AuditAction.AUTH_REGISTER : null;
        }
        if (path.startsWith("/cnpj/import") && HttpMethod.POST.matches(method) && !path.contains("/historico")) {
            return status >= 200 && status < 300 ? AuditAction.CNPJ_IMPORT : null;
        }
        if (path.matches(".*/import/[^/]+$") && HttpMethod.DELETE.matches(method)) {
            return AuditAction.CNPJ_CANCEL;
        }
        if (path.matches(".*/import/[^/]+/download$") && HttpMethod.GET.matches(method)) {
            return AuditAction.CNPJ_DOWNLOAD;
        }
        if (path.startsWith("/cnpj/preview") && HttpMethod.GET.matches(method) && !path.endsWith("/quota")) {
            return status >= 200 && status < 300 ? AuditAction.CNPJ_PREVIEW : null;
        }
        if (status == 401 || status == 403) {
            if (path.startsWith("/cnpj") || path.startsWith("/auth/me")) {
                return AuditAction.ACCESS_DENIED;
            }
        }
        return null;
    }

    private String montarDetalhes(String method, String path, int status) {
        if (status == 401) {
            return "Não autenticado";
        }
        if (status == 403) {
            return "Acesso negado";
        }
        return method + " " + path;
    }

    private UUID extrairUserId() {
        return SecurityUtils.currentUserIdOrNull();
    }
}
