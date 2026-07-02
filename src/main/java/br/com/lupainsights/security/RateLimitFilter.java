package br.com.lupainsights.security;

import br.com.lupainsights.config.SecurityProperties;
import br.com.lupainsights.util.IpRateLimiter;
import br.com.lupainsights.util.RequestIpResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String CLIENT_IP_ATTRIBUTE = "clientIp";

    private static final long HOUR_MS = 60L * 60 * 1000;
    private static final long MINUTE_MS = 60L * 1000;

    private final SecurityProperties securityProperties;
    private final IpRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final RequestIpResolver requestIpResolver;

    public RateLimitFilter(SecurityProperties securityProperties,
                           IpRateLimiter rateLimiter,
                           ObjectMapper objectMapper,
                           RequestIpResolver requestIpResolver) {
        this.securityProperties = securityProperties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.requestIpResolver = requestIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = PublicApiRoutes.normalizePath(request.getRequestURI());
        return path.startsWith("/actuator") || path.equals("/health") || path.equals("/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = PublicApiRoutes.normalizePath(request.getRequestURI());
        String method = request.getMethod();
        String clientIp = resolverClientIp(request);
        request.setAttribute(CLIENT_IP_ATTRIBUTE, clientIp);

        RateLimitRule rule = resolverRegraIp(path, method);
        if (rule != null) {
            String key = "ip:" + clientIp + ":" + rule.suffix();
            if (!rateLimiter.tryAcquire(key, rule.maxRequests(), rule.windowMs())) {
                responder429(response, rule.retryAfterSeconds());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    String resolverClientIp(HttpServletRequest request) {
        Object atributo = request.getAttribute(CLIENT_IP_ATTRIBUTE);
        if (atributo instanceof String ip && !ip.isBlank()) {
            return ip;
        }
        return requestIpResolver.resolver(request);
    }

    private RateLimitRule resolverRegraIp(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && path.equals("/cnpj/import")) {
            return new RateLimitRule(securityProperties.getImportPerHour(), HOUR_MS, 3600, "import");
        }
        if ("GET".equalsIgnoreCase(method) && path.equals("/cnpj/template")) {
            return new RateLimitRule(securityProperties.getTemplatePerHour(), HOUR_MS, 3600, "template");
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+/status")) {
            return new RateLimitRule(securityProperties.getStatusPerMinute(), MINUTE_MS, 60, "status");
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+/download")) {
            return new RateLimitRule(securityProperties.getDownloadPerHour(), HOUR_MS, 3600, "download");
        }
        if ("DELETE".equalsIgnoreCase(method) && path.matches("/cnpj/import/[^/]+")) {
            return new RateLimitRule(securityProperties.getStatusPerMinute(), MINUTE_MS, 60, "cancel");
        }
        if ("POST".equalsIgnoreCase(method) && path.matches("/auth/(login|register)")) {
            return new RateLimitRule(securityProperties.getAuthPerMinute(), MINUTE_MS, 60, "auth");
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/auth/forgot-password")) {
            return new RateLimitRule(securityProperties.getPasswordResetForgotPerHour(), HOUR_MS, 3600, "forgot-password");
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/auth/reset-password")) {
            return new RateLimitRule(securityProperties.getPasswordResetPerMinute(), MINUTE_MS, 60, "reset-password");
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/auth/bootstrap-admin")) {
            return new RateLimitRule(securityProperties.getAdminBootstrapPerHour(), HOUR_MS, 3600, "bootstrap-admin");
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/auth/resend-verification")) {
            return new RateLimitRule(securityProperties.getEmailVerificationResendPerHour(), HOUR_MS, 3600, "resend-verification");
        }
        if ("PUT".equalsIgnoreCase(method) && path.equals("/auth/password")) {
            return new RateLimitRule(securityProperties.getPasswordChangePerHour(), HOUR_MS, 3600, "password");
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/cnpj/preview")) {
            return new RateLimitRule(securityProperties.getGuestPreviewPerMinute(), MINUTE_MS, 60, "preview");
        }
        if (path.startsWith("/payments") && !path.contains("/mercadopago/webhook")) {
            return new RateLimitRule(securityProperties.getPaymentsPerMinute(), MINUTE_MS, 60, "payments");
        }
        if (path.startsWith("/analytics")) {
            return new RateLimitRule(securityProperties.getAnalyticsPerMinute(), MINUTE_MS, 60, "analytics");
        }
        if ("GET".equalsIgnoreCase(method) && (path.equals("/cnpj/import/historico")
                || path.matches("/cnpj/import/historico/[^/]+")
                || path.equals("/cnpj/import/listas-salvas")
                || path.equals("/cnpj/import/ativo"))) {
            return new RateLimitRule(securityProperties.getHistoricoPerMinute(), MINUTE_MS, 60, "historico");
        }
        if ("GET".equalsIgnoreCase(method) && path.equals("/auth/me")) {
            return new RateLimitRule(securityProperties.getReadPerMinute(), MINUTE_MS, 60, "me");
        }
        if ("GET".equalsIgnoreCase(method) && path.equals("/cnpj/config")) {
            return new RateLimitRule(securityProperties.getReadPerMinute(), MINUTE_MS, 60, "config");
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/cnpj/consulta")) {
            return new RateLimitRule(securityProperties.getConsultaPerMinute(), MINUTE_MS, 60, "consulta");
        }
        if (path.startsWith("/admin")) {
            return new RateLimitRule(securityProperties.getAdminPerMinute(), MINUTE_MS, 60, "admin");
        }
        return null;
    }

    private void responder429(HttpServletResponse response, int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "erro", "Muitas requisições. Aguarde alguns instantes e tente novamente."
        ));
    }

    record RateLimitRule(int maxRequests, long windowMs, int retryAfterSeconds, String suffix) {
    }
}
